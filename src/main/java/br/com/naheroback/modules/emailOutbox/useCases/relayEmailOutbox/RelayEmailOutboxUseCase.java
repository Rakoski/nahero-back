package br.com.naheroback.modules.emailOutbox.useCases.relayEmailOutbox;

import br.com.naheroback.modules.auth.services.PasswordResetService;
import br.com.naheroback.modules.emailOutbox.entities.EmailOutbox;
import br.com.naheroback.modules.emailOutbox.entities.enums.EmailOutboxStatus;
import br.com.naheroback.modules.emailOutbox.repositories.EmailOutboxRepository;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.services.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Slf4j(topic = "EMAIL_OUTBOX")
@Service
@RequiredArgsConstructor
public class RelayEmailOutboxUseCase {

    private static final int LAST_ERROR_MAX_LENGTH = 500;

    private final EmailOutboxRepository emailOutboxRepository;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;
    private final TransactionTemplate transactionTemplate;

    @Value("${email-outbox.enabled:true}")
    private boolean scheduledEnabled;

    @Value("${email-outbox.batch-size:50}")
    private int batchSize;

    @Value("${email-outbox.max-attempts:5}")
    private int maxAttempts;

    @Value("${email-outbox.base-backoff-seconds:30}")
    private long baseBackoffSeconds;

    @Scheduled(
            fixedDelayString = "${email-outbox.fixed-delay-ms:5000}",
            initialDelayString = "${email-outbox.initial-delay-ms:10000}"
    )
    public void runScheduled() {
        if (!scheduledEnabled) return;

        try {
            RelayEmailOutboxResponse summary = execute();
            if (summary.total() > 0) {
                log.info("Email outbox relay: sent={} retrying={} failed={} skipped={}", summary.sent(), summary.retrying(), summary.failed(), summary.skipped());
            }
        } catch (RuntimeException e) {
            log.error("Email outbox relay aborted", e);
        }
    }

    public RelayEmailOutboxResponse execute() {
        int sent = 0;
        int retrying = 0;
        int failed = 0;
        int skipped = 0;

        for (int processed = 0; processed < batchSize; processed++) {
            Optional<EmailOutboxStatus> outcome;

            try {
                outcome = transactionTemplate.execute(transaction -> emailOutboxRepository.lockNextDue(LocalDateTime.now()).map(this::relay));
            } catch (RuntimeException e) {
                log.error("Could not commit an email outbox entry, leaving the rest for the next run", e);
                break;
            }

            if (Objects.isNull(outcome) || outcome.isEmpty()) break;

            switch (outcome.get()) {
                case SENT -> sent++;
                case PENDING -> retrying++;
                case FAILED -> failed++;
                case SKIPPED -> skipped++;
            }
        }

        return new RelayEmailOutboxResponse(sent, retrying, failed, skipped);
    }

    private EmailOutboxStatus relay(EmailOutbox email) {
        LocalDateTime now = LocalDateTime.now();
        email.setAttempts(email.getAttempts() + 1);

        try {
            boolean delivered = deliver(email);
            email.setStatus(delivered ? EmailOutboxStatus.SENT : EmailOutboxStatus.SKIPPED);
            email.setSentAt(delivered ? now : null);
            email.setLastError(null);
        } catch (RuntimeException e) {
            email.setLastError(lastError(e));

            if (email.getAttempts() >= maxAttempts) {
                email.setStatus(EmailOutboxStatus.FAILED);
                log.error("Giving up on the {} email {} after {} attempts", email.getEmailType(), email.getId(), email.getAttempts(), e);
            } else {
                email.setNextAttemptAt(now.plus(backoff(email.getAttempts())));
                log.warn("Could not send the {} email {} (attempt {}), retrying at {}", email.getEmailType(), email.getId(), email.getAttempts(), email.getNextAttemptAt(), e);
            }
        }

        emailOutboxRepository.save(email);
        return email.getStatus();
    }

    private boolean deliver(EmailOutbox email) {
        User user = email.getUser();
        Locale locale = Locale.forLanguageTag(email.getLocale());

        return switch (email.getEmailType()) {
            case PASSWORD_RESET -> {
                passwordResetService.issueAndSend(user, locale);
                yield true;
            }
            case EMAIL_VERIFICATION -> {
                if (user.isEmailConfirmed()) yield false;
                emailVerificationService.issueAndSend(user, locale);
                yield true;
            }
        };
    }

    private Duration backoff(int attempts) {
        return Duration.ofSeconds(baseBackoffSeconds).multipliedBy(1L << (attempts - 1));
    }

    private String lastError(RuntimeException e) {
        Throwable root = e;

        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        String reason = "%s: %s".formatted(root.getClass().getSimpleName(), root.getMessage());

        return reason.length() > LAST_ERROR_MAX_LENGTH
                ? reason.substring(0, LAST_ERROR_MAX_LENGTH)
                : reason;
    }
}

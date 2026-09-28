package br.com.naheroback.modules.reengagement.useCases.dispatchReengagementEmails;

import br.com.naheroback.common.services.EmailService;
import br.com.naheroback.common.utils.SecureTokenGenerator;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import br.com.naheroback.modules.reengagement.entities.ReengagementDispatchRun;
import br.com.naheroback.modules.reengagement.entities.ReengagementEmail;
import br.com.naheroback.modules.reengagement.entities.enums.ReengagementEmailStatus;
import br.com.naheroback.modules.reengagement.entities.enums.ReengagementEmailType;
import br.com.naheroback.modules.reengagement.repositories.ReengagementCandidate;
import br.com.naheroback.modules.reengagement.repositories.ReengagementDispatchRunRepository;
import br.com.naheroback.modules.reengagement.repositories.ReengagementEmailHistory;
import br.com.naheroback.modules.reengagement.repositories.ReengagementEmailRepository;
import br.com.naheroback.modules.reengagement.repositories.ReengagementUserRepository;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j(topic = "REENGAGEMENT")
@Service
@RequiredArgsConstructor
public class DispatchReengagementEmailsUseCase {

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("pt", "en");
    private static final String FALLBACK_LANGUAGE = "en";
    private static final int LANGUAGE_LOOKBACK_ATTEMPTS = 10;
    private static final int FAILURE_REASON_MAX_LENGTH = 500;

    private final ReengagementUserRepository reengagementUserRepository;
    private final ReengagementEmailRepository reengagementEmailRepository;
    private final ReengagementDispatchRunRepository reengagementDispatchRunRepository;
    private final StudentPracticeAttemptRepository studentPracticeAttemptRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.api.url}")
    private String apiUrl;

    @Value("${reengagement.enabled:true}")
    private boolean scheduledEnabled;

    @Value("${reengagement.batch-size:200}")
    private int batchSize;

    @Value("${reengagement.campaign-months:3}")
    private int campaignMonths;

    @Value("${reengagement.min-days-between-emails:7}")
    private int minDaysBetweenEmails;

    @Value("${reengagement.max-attempts-per-step:3}")
    private int maxAttemptsPerStep;

    @Value("${reengagement.default-language:pt}")
    private String defaultLanguage;

    @Scheduled(
            fixedDelayString = "${reengagement.fixed-delay-ms:3600000}",
            initialDelayString = "${reengagement.initial-delay-ms:120000}"
    )
    public void runScheduled() {
        if (!scheduledEnabled) {
            log.info("Re-engagement dispatch is disabled, skipping the scan");
            return;
        }

        try {
            DispatchReengagementEmailsResponse summary = execute();
            log.info("Re-engagement dispatch (scheduled): candidates={} sent={} failed={} skipped={}", summary.candidates(), summary.sent(), summary.failed(), summary.skipped());
        } catch (RuntimeException e) {
            log.error("Re-engagement dispatch (scheduled) aborted", e);
        }
    }

    public DispatchReengagementEmailsResponse execute() {
        LocalDateTime startedAt = LocalDateTime.now();
        LocalDateTime campaignStart = startedAt.minusMonths(campaignMonths);
        LocalDateTime inactiveSince = startedAt.minus(ReengagementEmailType.first().getDelayAfterLastActivity());
        LocalDateTime cooldownStart = startedAt.minusDays(minDaysBetweenEmails);

        List<ReengagementCandidate> candidates = reengagementUserRepository.findCampaignCandidates(
                campaignStart, inactiveSince, cooldownStart, ReengagementEmailStatus.SENT,
                PageRequest.of(0, batchSize));

        DispatchReengagementEmailsResponse summary = dispatchAll(candidates, campaignStart, startedAt);

        recordRun(startedAt, summary, campaignStart, inactiveSince, cooldownStart);

        return summary;
    }

    private DispatchReengagementEmailsResponse dispatchAll(List<ReengagementCandidate> candidates,
                                                           LocalDateTime campaignStart, LocalDateTime now) {
        if (candidates.isEmpty()) return new DispatchReengagementEmailsResponse(0, 0, 0, 0);

        Map<Integer, List<ReengagementEmailHistory>> historyByUser = loadHistory(candidates, campaignStart);

        int sent = 0;
        int failed = 0;
        int skipped = 0;

        for (ReengagementCandidate candidate : candidates) {
            List<ReengagementEmailHistory> history = historyByUser.getOrDefault(candidate.getUserId(), List.of());
            Optional<ReengagementEmailType> step = nextStep(candidate, history, now);

            if (step.isEmpty()) {
                skipped++;
                log.debug("Skipping user {}: no step is due (last activity {})", candidate.getUserId(), candidate.getLastActivityAt());
                continue;
            }

            try {
                dispatch(candidate, step.get(), now);
                sent++;
            } catch (RuntimeException e) {
                failed++;
                log.error("Could not send the {} re-engagement email to user {}", step.get(), candidate.getUserId(), e);
            }
        }

        return new DispatchReengagementEmailsResponse(candidates.size(), sent, failed, skipped);
    }

    private void recordRun(LocalDateTime startedAt, DispatchReengagementEmailsResponse summary, LocalDateTime campaignStart, LocalDateTime inactiveSince, LocalDateTime cooldownStart) {
        try {
            LocalDateTime finishedAt = LocalDateTime.now();

            ReengagementDispatchRun run = new ReengagementDispatchRun();
            run.setStartedAt(startedAt);
            run.setFinishedAt(finishedAt);
            run.setDurationMs(Duration.between(startedAt, finishedAt).toMillis());
            run.setCandidates(summary.candidates());
            run.setSent(summary.sent());
            run.setFailed(summary.failed());
            run.setSkipped(summary.skipped());
            run.setFunnel(reengagementUserRepository.findEligibilityFunnel(campaignStart, inactiveSince, cooldownStart));

            reengagementDispatchRunRepository.save(run);
        } catch (RuntimeException e) {
            log.warn("Could not record the re-engagement dispatch run", e);
        }
    }

    private Map<Integer, List<ReengagementEmailHistory>> loadHistory(List<ReengagementCandidate> candidates,
                                                                    LocalDateTime campaignStart) {
        List<Integer> userIds = candidates.stream()
                .map(ReengagementCandidate::getUserId)
                .toList();

        return reengagementEmailRepository.findHistoryForUsers(userIds, campaignStart).stream()
                .collect(Collectors.groupingBy(ReengagementEmailHistory::getUserId));
    }

    private Optional<ReengagementEmailType> nextStep(ReengagementCandidate candidate, List<ReengagementEmailHistory> history, LocalDateTime now) {
        List<ReengagementEmailHistory> campaign = history.stream()
                .filter(email -> email.getSentAt().isAfter(candidate.getLastActivityAt()))
                .toList();

        Set<ReengagementEmailType> delivered = campaign.stream()
                .filter(email -> email.getStatus() == ReengagementEmailStatus.SENT)
                .map(ReengagementEmailHistory::getEmailType)
                .collect(Collectors.toSet());

        Map<ReengagementEmailType, Long> failures = campaign.stream()
                .filter(email -> email.getStatus() == ReengagementEmailStatus.FAILED)
                .collect(Collectors.groupingBy(ReengagementEmailHistory::getEmailType, Collectors.counting()));

        return ReengagementEmailType.sequence().stream()
                .filter(type -> !delivered.contains(type))
                .filter(type -> failures.getOrDefault(type, 0L) < maxAttemptsPerStep)
                .findFirst()
                .filter(type -> !now.isBefore(candidate.getLastActivityAt().plus(type.getDelayAfterLastActivity())));
    }

    private void dispatch(ReengagementCandidate candidate, ReengagementEmailType type, LocalDateTime now) {
        User user = userRepository.findById(candidate.getUserId()).orElseThrow();
        Locale locale = resolveLocale(candidate.getUserId());

        String actionLink = "%s/%s%s".formatted(frontendUrl, locale.getLanguage(), type.getLandingPath());

        ReengagementEmail dispatched = new ReengagementEmail();
        dispatched.setUser(user);
        dispatched.setEmailType(type);
        dispatched.setSentAt(now);
        dispatched.setCampaignStartedAt(candidate.getLastActivityAt());
        dispatched.setStatus(ReengagementEmailStatus.SENT);
        reengagementEmailRepository.save(dispatched);

        try {
            emailService.sendReengagementEmail(user.getEmail(), user.getName(), type.messagePrefix(), actionLink, locale);
        } catch (RuntimeException e) {
            dispatched.setStatus(ReengagementEmailStatus.FAILED);
            dispatched.setFailureReason(failureReason(e));
            reengagementEmailRepository.save(dispatched);
            throw e;
        }
    }

    private String failureReason(RuntimeException e) {
        Throwable root = e;

        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        String reason = "%s: %s".formatted(root.getClass().getSimpleName(), root.getMessage());

        return reason.length() > FAILURE_REASON_MAX_LENGTH
                ? reason.substring(0, FAILURE_REASON_MAX_LENGTH)
                : reason;
    }

    private Locale resolveLocale(Integer userId) {
        return studentPracticeAttemptRepository
                .findRecentLanguagesForStudent(userId, PageRequest.of(0, LANGUAGE_LOOKBACK_ATTEMPTS)).stream()
                .map(this::supportedLanguage)
                .flatMap(Optional::stream)
                .findFirst()
                .or(() -> supportedLanguage(defaultLanguage))
                .map(Locale::forLanguageTag)
                .orElseGet(() -> Locale.forLanguageTag(FALLBACK_LANGUAGE));
    }

    private Optional<String> supportedLanguage(String language) {
        if (language == null || language.isBlank()) return Optional.empty();

        String tag = Locale.forLanguageTag(language.trim().replace('_', '-')).getLanguage();

        return SUPPORTED_LANGUAGES.contains(tag) ? Optional.of(tag) : Optional.empty();
    }
}

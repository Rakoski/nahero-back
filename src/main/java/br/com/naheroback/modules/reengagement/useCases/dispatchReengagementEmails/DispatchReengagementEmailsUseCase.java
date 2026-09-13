package br.com.naheroback.modules.reengagement.useCases.dispatchReengagementEmails;

import br.com.naheroback.common.services.EmailService;
import br.com.naheroback.common.utils.SecureTokenGenerator;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import br.com.naheroback.modules.reengagement.entities.ReengagementEmail;
import br.com.naheroback.modules.reengagement.entities.enums.ReengagementEmailType;
import br.com.naheroback.modules.reengagement.repositories.ReengagementCandidate;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    private final ReengagementUserRepository reengagementUserRepository;
    private final ReengagementEmailRepository reengagementEmailRepository;
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

    @Value("${reengagement.default-language:pt}")
    private String defaultLanguage;

    @Scheduled(
            fixedDelayString = "${reengagement.fixed-delay-ms:3600000}",
            initialDelayString = "${reengagement.initial-delay-ms:120000}"
    )
    public void runScheduled() {
        if (!scheduledEnabled) return;
        DispatchReengagementEmailsResponse summary = execute();
        log.info("Re-engagement dispatch (scheduled): candidates={}", summary.candidates());
    }

    public DispatchReengagementEmailsResponse execute() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime campaignStart = now.minusMonths(campaignMonths);
        LocalDateTime inactiveSince = now.minus(ReengagementEmailType.first().getDelayAfterLastActivity());
        LocalDateTime cooldownStart = now.minusDays(minDaysBetweenEmails);

        List<ReengagementCandidate> candidates = reengagementUserRepository.findCampaignCandidates(campaignStart, inactiveSince, cooldownStart, PageRequest.of(0, batchSize));

        if (candidates.isEmpty()) return new DispatchReengagementEmailsResponse(0);

        Map<Integer, List<ReengagementEmail>> historyByUser = loadHistory(candidates, campaignStart);

        for (ReengagementCandidate candidate : candidates) {
            List<ReengagementEmail> history = historyByUser.getOrDefault(candidate.getUserId(), List.of());
            Optional<ReengagementEmailType> step = nextStep(candidate, history, now);

            if (step.isEmpty()) continue;

            try {
                dispatch(candidate, step.get(), now);
            } catch (RuntimeException e) {
                log.error("Could not send the {} re-engagement email to user {}", step.get(), candidate.getUserId(), e);
            }
        }

        return new DispatchReengagementEmailsResponse(candidates.size());
    }

    private Map<Integer, List<ReengagementEmail>> loadHistory(List<ReengagementCandidate> candidates,
                                                              LocalDateTime campaignStart) {
        List<Integer> userIds = candidates.stream()
                .map(ReengagementCandidate::getUserId)
                .toList();

        return reengagementEmailRepository.findByUserIdInAndSentAtAfter(userIds, campaignStart).stream()
                .collect(Collectors.groupingBy(email -> email.getUser().getId()));
    }

    private Optional<ReengagementEmailType> nextStep(ReengagementCandidate candidate, List<ReengagementEmail> history, LocalDateTime now) {
        Set<ReengagementEmailType> alreadySent = history.stream()
                .filter(email -> email.getSentAt().isAfter(candidate.getLastActivityAt()))
                .map(ReengagementEmail::getEmailType)
                .collect(Collectors.toSet());

        return ReengagementEmailType.sequence().stream()
                .filter(type -> !alreadySent.contains(type))
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
        reengagementEmailRepository.save(dispatched);

        emailService.sendReengagementEmail(user.getEmail(), user.getName(), type.messagePrefix(),
                actionLink, locale);
    }

    private String ensureUnsubscribeToken(User user) {
        String token = user.getReengagementUnsubscribeToken();

        if (token != null && !token.isBlank()) return token;

        user.setReengagementUnsubscribeToken(SecureTokenGenerator.generate());
        userRepository.save(user);

        return user.getReengagementUnsubscribeToken();
    }

    private Locale resolveLocale(Integer userId) {
        return studentPracticeAttemptRepository.findRecentLanguagesForStudent(userId, PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(Locale::forLanguageTag)
                .orElseGet(() -> Locale.forLanguageTag(defaultLanguage));
    }
}

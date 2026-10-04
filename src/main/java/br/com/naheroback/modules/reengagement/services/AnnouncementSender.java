package br.com.naheroback.modules.reengagement.services;

import br.com.naheroback.common.services.EmailService;
import br.com.naheroback.common.utils.SecureTokenGenerator;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import br.com.naheroback.modules.reengagement.entities.AnnouncementEmail;
import br.com.naheroback.modules.reengagement.entities.enums.AnnouncementCampaign;
import br.com.naheroback.modules.reengagement.entities.enums.ReengagementEmailStatus;
import br.com.naheroback.modules.reengagement.repositories.AnnouncementEmailRepository;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Slf4j(topic = "ANNOUNCEMENT")
@Service
@RequiredArgsConstructor
public class AnnouncementSender {

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("pt", "en");
    private static final int LANGUAGE_LOOKBACK_ATTEMPTS = 10;
    private static final int FAILURE_REASON_MAX_LENGTH = 500;

    private final AnnouncementEmailRepository announcementEmailRepository;
    private final UserRepository userRepository;
    private final StudentPracticeAttemptRepository studentPracticeAttemptRepository;
    private final EmailService emailService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.api.url}")
    private String apiUrl;

    @Value("${reengagement.default-language:pt}")
    private String defaultLanguage;

    @Async
    public void sendAll(List<Integer> userIds, AnnouncementCampaign campaign) {
        int sent = 0;
        int failed = 0;

        for (Integer userId : userIds) {
            if (send(userId, campaign)) sent++;
            else failed++;
        }

        log.info("Announcement {} finished: recipients={} sent={} failed={}", campaign, userIds.size(), sent, failed);
    }

    public boolean send(Integer userId, AnnouncementCampaign campaign) {
        Optional<User> found = userRepository.findById(userId);
        if (found.isEmpty()) return false;

        User user = found.get();
        Locale locale = resolveLocale(userId);

        AnnouncementEmail record = announcementEmailRepository.findByUserIdAndCampaign(userId, campaign.getSlug())
                .orElseGet(AnnouncementEmail::new);
        record.setUser(user);
        record.setCampaign(campaign.getSlug());
        record.setStatus(ReengagementEmailStatus.SENT);
        record.setSentAt(LocalDateTime.now());
        record.setFailureReason(null);
        announcementEmailRepository.save(record);

        try {
            deliver(user, campaign, locale);
            return true;
        } catch (RuntimeException e) {
            record.setStatus(ReengagementEmailStatus.FAILED);
            record.setFailureReason(failureReason(e));
            announcementEmailRepository.save(record);
            log.error("Could not send the {} announcement to user {}", campaign, userId, e);
            return false;
        }
    }

    public Locale sendTest(User user, AnnouncementCampaign campaign, String language) {
        Locale locale = supportedLanguage(language)
                .map(Locale::forLanguageTag)
                .orElseGet(() -> resolveLocale(user.getId()));

        deliver(user, campaign, locale);
        return locale;
    }

    private void deliver(User user, AnnouncementCampaign campaign, Locale locale) {
        emailService.sendAnnouncementEmail(user.getEmail(), user.getName(), campaign.messagePrefix(), locale,
                actionLink(campaign, locale), unsubscribeLink(user));
    }

    private String actionLink(AnnouncementCampaign campaign, Locale locale) {
        return "%s/%s%s?utm_source=email&utm_medium=announcement&utm_campaign=%s"
                .formatted(frontendUrl, locale.getLanguage(), campaign.getLandingPath(), campaign.getSlug());
    }

    private String unsubscribeLink(User user) {
        if (user.getReengagementUnsubscribeToken() == null) {
            user.setReengagementUnsubscribeToken(SecureTokenGenerator.generate());
            userRepository.save(user);
        }

        return "%s/reengagement/unsubscribe?token=%s".formatted(apiUrl,
                URLEncoder.encode(user.getReengagementUnsubscribeToken(), StandardCharsets.UTF_8));
    }

    private Locale resolveLocale(Integer userId) {
        return studentPracticeAttemptRepository
                .findRecentLanguagesForStudent(userId, PageRequest.of(0, LANGUAGE_LOOKBACK_ATTEMPTS)).stream()
                .map(this::supportedLanguage)
                .flatMap(Optional::stream)
                .findFirst()
                .or(() -> supportedLanguage(defaultLanguage))
                .map(Locale::forLanguageTag)
                .orElseGet(() -> Locale.forLanguageTag("pt"));
    }

    private Optional<String> supportedLanguage(String language) {
        if (language == null || language.isBlank()) return Optional.empty();

        String tag = Locale.forLanguageTag(language.trim().replace('_', '-')).getLanguage();

        return SUPPORTED_LANGUAGES.contains(tag) ? Optional.of(tag) : Optional.empty();
    }

    private String failureReason(RuntimeException e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        String reason = "%s: %s".formatted(root.getClass().getSimpleName(), root.getMessage());
        return reason.length() > FAILURE_REASON_MAX_LENGTH ? reason.substring(0, FAILURE_REASON_MAX_LENGTH) : reason;
    }
}

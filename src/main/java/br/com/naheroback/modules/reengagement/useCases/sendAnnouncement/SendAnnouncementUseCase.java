package br.com.naheroback.modules.reengagement.useCases.sendAnnouncement;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.reengagement.entities.enums.AnnouncementCampaign;
import br.com.naheroback.modules.reengagement.entities.enums.ReengagementEmailStatus;
import br.com.naheroback.modules.reengagement.repositories.AnnouncementEmailRepository;
import br.com.naheroback.modules.reengagement.services.AnnouncementSender;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Slf4j(topic = "ANNOUNCEMENT")
@Service
@RequiredArgsConstructor
public class SendAnnouncementUseCase {
    private final AnnouncementEmailRepository announcementEmailRepository;
    private final AnnouncementSender announcementSender;
    private final UserRepository userRepository;

    @Secured("IS_ADMIN")
    public SendAnnouncementResponse execute(AnnouncementCampaign campaign, boolean dryRun) {
        List<Integer> recipients = announcementEmailRepository.findRecipientIds(campaign.getSlug(), ReengagementEmailStatus.SENT);

        if (!dryRun && !recipients.isEmpty()) {
            log.info("Sending the {} announcement to {} recipients", campaign, recipients.size());
            announcementSender.sendAll(recipients, campaign);
        }

        return new SendAnnouncementResponse(campaign.getSlug(), recipients.size(), dryRun);
    }

    @Secured("IS_ADMIN")
    public SendAnnouncementTestResponse sendTest(AnnouncementCampaign campaign, SendAnnouncementTestRequest request) {
        User user = userRepository.findByEmail(request.email().trim())
                .orElseThrow(() -> NotFoundException.with(User.class, "email", request.email()));

        Locale locale = announcementSender.sendTest(user, campaign, request.language());
        log.info("Sent a test of the {} announcement to user {} in {}", campaign, user.getId(), locale.getLanguage());

        return new SendAnnouncementTestResponse(campaign.getSlug(), user.getEmail(), locale.getLanguage());
    }
}

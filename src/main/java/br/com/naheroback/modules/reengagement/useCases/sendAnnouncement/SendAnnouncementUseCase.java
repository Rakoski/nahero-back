package br.com.naheroback.modules.reengagement.useCases.sendAnnouncement;

import br.com.naheroback.modules.reengagement.entities.enums.AnnouncementCampaign;
import br.com.naheroback.modules.reengagement.entities.enums.ReengagementEmailStatus;
import br.com.naheroback.modules.reengagement.repositories.AnnouncementEmailRepository;
import br.com.naheroback.modules.reengagement.services.AnnouncementSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j(topic = "ANNOUNCEMENT")
@Service
@RequiredArgsConstructor
public class SendAnnouncementUseCase {
    private final AnnouncementEmailRepository announcementEmailRepository;
    private final AnnouncementSender announcementSender;

    @Secured("IS_ADMIN")
    public SendAnnouncementResponse execute(AnnouncementCampaign campaign, boolean dryRun) {
        List<Integer> recipients = announcementEmailRepository.findRecipientIds(campaign.getSlug(), ReengagementEmailStatus.SENT);

        if (!dryRun && !recipients.isEmpty()) {
            log.info("Sending the {} announcement to {} recipients", campaign, recipients.size());
            announcementSender.sendAll(recipients, campaign);
        }

        return new SendAnnouncementResponse(campaign.getSlug(), recipients.size(), dryRun);
    }
}

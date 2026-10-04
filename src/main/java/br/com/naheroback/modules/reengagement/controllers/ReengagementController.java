package br.com.naheroback.modules.reengagement.controllers;

import br.com.naheroback.modules.reengagement.entities.enums.AnnouncementCampaign;
import br.com.naheroback.modules.reengagement.useCases.sendAnnouncement.SendAnnouncementResponse;
import br.com.naheroback.modules.reengagement.useCases.sendAnnouncement.SendAnnouncementUseCase;
import br.com.naheroback.modules.reengagement.useCases.unsubscribe.UnsubscribeFromReengagementRequest;
import br.com.naheroback.modules.reengagement.useCases.unsubscribe.UnsubscribeFromReengagementUseCase;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reengagement")
public class ReengagementController {

    private final UnsubscribeFromReengagementUseCase unsubscribeFromReengagementUseCase;
    private final SendAnnouncementUseCase sendAnnouncementUseCase;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @GetMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@RequestParam @NotBlank String token) {
        this.unsubscribeFromReengagementUseCase.execute(new UnsubscribeFromReengagementRequest(token));

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/?unsubscribed=true"))
                .build();
    }

    @PostMapping("/announcements/{campaign}")
    public ResponseEntity<SendAnnouncementResponse> sendAnnouncement(@PathVariable AnnouncementCampaign campaign, @RequestParam(defaultValue = "true") boolean dryRun) {
        SendAnnouncementResponse response = sendAnnouncementUseCase.execute(campaign, dryRun);
        return ResponseEntity.status(dryRun ? HttpStatus.OK : HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/unsubscribe")
    @ResponseStatus(HttpStatus.OK)
    public void unsubscribeOneClick(@RequestParam @NotBlank String token) {
        this.unsubscribeFromReengagementUseCase.execute(new UnsubscribeFromReengagementRequest(token));
    }
}

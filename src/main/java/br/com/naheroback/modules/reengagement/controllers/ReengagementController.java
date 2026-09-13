package br.com.naheroback.modules.reengagement.controllers;

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

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @GetMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@RequestParam @NotBlank String token) {
        this.unsubscribeFromReengagementUseCase.execute(new UnsubscribeFromReengagementRequest(token));

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/?unsubscribed=true"))
                .build();
    }

    @PostMapping("/unsubscribe")
    @ResponseStatus(HttpStatus.OK)
    public void unsubscribeOneClick(@RequestParam @NotBlank String token) {
        this.unsubscribeFromReengagementUseCase.execute(new UnsubscribeFromReengagementRequest(token));
    }
}

package br.com.naheroback.modules.subscription.controllers;

import br.com.naheroback.modules.subscription.useCases.cancel.CancelSubscriptionResponse;
import br.com.naheroback.modules.subscription.useCases.cancel.CancelSubscriptionUseCase;
import br.com.naheroback.modules.subscription.useCases.getStatus.GetSubscriptionStatusResponse;
import br.com.naheroback.modules.subscription.useCases.getStatus.GetSubscriptionStatusUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/subscription")
public class SubscriptionController {

    private final CancelSubscriptionUseCase cancelSubscriptionUseCase;
    private final GetSubscriptionStatusUseCase getSubscriptionStatusUseCase;

    @GetMapping
    public ResponseEntity<GetSubscriptionStatusResponse> get() {
        return ResponseEntity.ok(getSubscriptionStatusUseCase.execute());
    }

    @PostMapping("/cancel")
    public ResponseEntity<CancelSubscriptionResponse> cancel() {
        return ResponseEntity.ok(cancelSubscriptionUseCase.execute());
    }
}

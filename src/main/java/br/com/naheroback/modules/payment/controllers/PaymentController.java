package br.com.naheroback.modules.payment.controllers;

import br.com.naheroback.modules.payment.useCases.createCheckoutSession.CreateCheckoutSessionRequest;
import br.com.naheroback.modules.payment.useCases.createCheckoutSession.CreateCheckoutSessionResponse;
import br.com.naheroback.modules.payment.useCases.createCheckoutSession.CreateCheckoutSessionUseCase;
import br.com.naheroback.modules.payment.useCases.processWebhook.ProcessStripeWebhookUseCase;
import br.com.naheroback.modules.payment.useCases.reconcilePaymentEvents.ReconcilePaymentEventsResponse;
import br.com.naheroback.modules.payment.useCases.reconcilePaymentEvents.ReconcilePaymentEventsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payment")
public class PaymentController {

    private final ProcessStripeWebhookUseCase processStripeWebhookUseCase;
    private final CreateCheckoutSessionUseCase createCheckoutSessionUseCase;
    private final ReconcilePaymentEventsUseCase reconcilePaymentEventsUseCase;

    @PostMapping("/stripe")
    public ResponseEntity<Void> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature
    ) {
        processStripeWebhookUseCase.execute(payload, signature);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/checkout/subscription")
    public ResponseEntity<CreateCheckoutSessionResponse> createSubscriptionCheckout(
            @Valid @RequestBody CreateCheckoutSessionRequest request
    ) {
        return ResponseEntity.ok(createCheckoutSessionUseCase.execute(request));
    }

    @PostMapping("/reconcile")
    public ResponseEntity<ReconcilePaymentEventsResponse> reconcile() {
        return ResponseEntity.ok(reconcilePaymentEventsUseCase.execute());
    }
}

package br.com.naheroback.modules.payment.useCases.createCheckoutSession;

import br.com.naheroback.providers.payment.PlanInterval;
import jakarta.validation.constraints.NotNull;

public record CreateCheckoutSessionRequest(
        @NotNull(message = "{payment.plan.required}")
        PlanInterval plan,
        String successUrl,
        String cancelUrl
) {
}

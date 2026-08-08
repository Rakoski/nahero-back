package br.com.naheroback.modules.payment.useCases.createCheckoutSession;

public record CreateCheckoutSessionResponse(
        String sessionId,
        String checkoutUrl
) {
}

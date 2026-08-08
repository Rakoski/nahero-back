package br.com.naheroback.providers.payment;

public record CheckoutSessionResult(
        String sessionId,
        String checkoutUrl
) {
}

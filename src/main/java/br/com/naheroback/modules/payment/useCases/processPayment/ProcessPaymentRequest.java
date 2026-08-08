package br.com.naheroback.modules.payment.useCases.processPayment;

import br.com.naheroback.providers.payment.PaymentProviderName;
import lombok.Builder;

import java.time.Instant;

@Builder
public record ProcessPaymentRequest(
        Kind kind,
        PaymentProviderName provider,
        String externalCustomerId,
        String externalSubscriptionId,
        Instant currentPeriodEnd,
        Boolean cancelAtPeriodEnd
) {

    public enum Kind {
        ACTIVATE_OR_RENEW,
        CANCEL
    }
}

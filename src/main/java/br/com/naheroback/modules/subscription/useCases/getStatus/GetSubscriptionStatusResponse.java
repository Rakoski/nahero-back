package br.com.naheroback.modules.subscription.useCases.getStatus;

import br.com.naheroback.modules.subscription.entities.SubscriptionStatus;
import br.com.naheroback.providers.payment.PaymentProviderName;

import java.time.Instant;

public record GetSubscriptionStatusResponse(
        boolean isPremium,
        Integer freeTriesLeft,
        SubscriptionStatus status,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        PaymentProviderName provider,
        String externalSubscriptionId
) {
}

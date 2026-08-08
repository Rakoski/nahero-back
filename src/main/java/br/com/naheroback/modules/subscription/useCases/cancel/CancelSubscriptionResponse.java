package br.com.naheroback.modules.subscription.useCases.cancel;

import java.time.Instant;

public record CancelSubscriptionResponse(
        String externalSubscriptionId,
        Instant accessExpiresAt
) {
}

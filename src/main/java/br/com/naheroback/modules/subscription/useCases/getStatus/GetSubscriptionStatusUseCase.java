package br.com.naheroback.modules.subscription.useCases.getStatus;

import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.subscription.entities.Subscription;
import br.com.naheroback.modules.subscription.repositories.SubscriptionRepository;
import br.com.naheroback.modules.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetSubscriptionStatusUseCase {

    private final SubscriptionRepository subscriptionRepository;

    public GetSubscriptionStatusResponse execute() {
        User user = AuthService.getUserFromToken();

        Optional<Subscription> current = subscriptionRepository
                .findFirstByUserIdOrderByCurrentPeriodEndDesc(user.getId());

        if (current.isEmpty()) {
            return new GetSubscriptionStatusResponse(
                    false, user.getFreeTriesLeft(), null, null, false, null, null
            );
        }

        Subscription subscription = current.get();
        boolean isPremium = subscription.getCurrentPeriodEnd() != null
                && subscription.getCurrentPeriodEnd().isAfter(Instant.now());

        return new GetSubscriptionStatusResponse(
                isPremium,
                user.getFreeTriesLeft(),
                subscription.getStatus(),
                subscription.getCurrentPeriodEnd(),
                subscription.isCancelAtPeriodEnd(),
                subscription.getProvider(),
                subscription.getExternalSubscriptionId()
        );
    }
}

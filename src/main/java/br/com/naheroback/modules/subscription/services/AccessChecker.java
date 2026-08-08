package br.com.naheroback.modules.subscription.services;

import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.subscription.repositories.SubscriptionRepository;
import br.com.naheroback.modules.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component("access")
@RequiredArgsConstructor
public class AccessChecker {

    private final SubscriptionRepository subscriptionRepository;

    public boolean isPremium() {
        User user = AuthService.getUserFromToken();
        return subscriptionRepository.findFirstByUserIdOrderByCurrentPeriodEndDesc(user.getId())
                .map(subscription -> subscription.getCurrentPeriodEnd() != null
                        && subscription.getCurrentPeriodEnd().isAfter(Instant.now()))
                .orElse(false);
    }
}

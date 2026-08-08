package br.com.naheroback.modules.subscription.useCases.cancel;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.common.exceptions.custom.UnprocessableEntityException;
import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.subscription.entities.Subscription;
import br.com.naheroback.modules.subscription.entities.SubscriptionStatus;
import br.com.naheroback.modules.subscription.repositories.SubscriptionRepository;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.providers.payment.PaymentProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CancelSubscriptionUseCase {

    private final PaymentProvider paymentProvider;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public CancelSubscriptionResponse execute() {
        User user = AuthService.getUserFromToken();

        Optional<Subscription> current = subscriptionRepository
                .findFirstByUserIdOrderByCurrentPeriodEndDesc(user.getId());

        Subscription subscription = current
                .orElseThrow(() -> NotFoundException.with(Subscription.class, "user_id", user.getId()));

        if (subscription.getStatus() == SubscriptionStatus.CANCELED) {
            throw new UnprocessableEntityException("subscription.already_canceled");
        }
        if (subscription.getCurrentPeriodEnd() != null
                && subscription.getCurrentPeriodEnd().isBefore(Instant.now())) {
            throw new UnprocessableEntityException("subscription.already_expired");
        }
        if (subscription.isCancelAtPeriodEnd()) {
            return new CancelSubscriptionResponse(
                    subscription.getExternalSubscriptionId(),
                    subscription.getCurrentPeriodEnd()
            );
        }

        paymentProvider.cancelSubscriptionAtPeriodEnd(subscription.getExternalSubscriptionId());
        subscription.setCancelAtPeriodEnd(true);
        subscriptionRepository.save(subscription);

        return new CancelSubscriptionResponse(
                subscription.getExternalSubscriptionId(),
                subscription.getCurrentPeriodEnd()
        );
    }
}

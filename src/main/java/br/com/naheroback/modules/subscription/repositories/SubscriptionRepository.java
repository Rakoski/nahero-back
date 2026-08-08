package br.com.naheroback.modules.subscription.repositories;

import br.com.naheroback.modules.subscription.entities.Subscription;
import br.com.naheroback.providers.payment.PaymentProviderName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {

    Optional<Subscription> findByProviderAndExternalSubscriptionId(
            PaymentProviderName provider,
            String externalSubscriptionId
    );

    Optional<Subscription> findFirstByUserIdOrderByCurrentPeriodEndDesc(Integer userId);
}

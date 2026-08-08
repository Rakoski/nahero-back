package br.com.naheroback.modules.payment.useCases.processPayment;

import br.com.naheroback.modules.payment.entities.EventProcessingStatus;
import br.com.naheroback.modules.subscription.entities.Subscription;
import br.com.naheroback.modules.subscription.entities.SubscriptionStatus;
import br.com.naheroback.modules.subscription.repositories.SubscriptionRepository;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCase {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    // Never returns FAILED — that state is decided by the caller based on retry attempts.
    @Transactional
    public EventProcessingStatus execute(ProcessPaymentRequest request) {
        Optional<Subscription> existing = subscriptionRepository
                .findByProviderAndExternalSubscriptionId(request.provider(), request.externalSubscriptionId());

        if (existing.isPresent()) {
            applyToExisting(existing.get(), request);
            return EventProcessingStatus.APPLIED;
        }

        if (request.kind() == ProcessPaymentRequest.Kind.CANCEL) {
            log.info("Cancel event for unknown subscription provider={} id={} — nothing to do",
                    request.provider(), request.externalSubscriptionId());
            return EventProcessingStatus.IGNORED;
        }

        Optional<User> userOpt = userRepository
                .findByPaymentProviderAndExternalCustomerId(request.provider(), request.externalCustomerId());
        if (userOpt.isEmpty()) {
            log.warn("Deferring event: no user found for provider={} customer={} subscription={}",
                    request.provider(), request.externalCustomerId(), request.externalSubscriptionId());
            return EventProcessingStatus.DEFERRED;
        }

        Subscription subscription = new Subscription();
        subscription.setUser(userOpt.get());
        subscription.setProvider(request.provider());
        subscription.setExternalSubscriptionId(request.externalSubscriptionId());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(request.currentPeriodEnd());
        subscription.setCancelAtPeriodEnd(Boolean.TRUE.equals(request.cancelAtPeriodEnd()));
        subscriptionRepository.save(subscription);
        return EventProcessingStatus.APPLIED;
    }

    private void applyToExisting(Subscription subscription, ProcessPaymentRequest request) {
        switch (request.kind()) {
            case ACTIVATE_OR_RENEW -> {
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                if (request.currentPeriodEnd() != null
                        && (subscription.getCurrentPeriodEnd() == null
                            || request.currentPeriodEnd().isAfter(subscription.getCurrentPeriodEnd()))) {
                    subscription.setCurrentPeriodEnd(request.currentPeriodEnd());
                }
                if (request.cancelAtPeriodEnd() != null) {
                    subscription.setCancelAtPeriodEnd(request.cancelAtPeriodEnd());
                }
                subscriptionRepository.save(subscription);
            }
            case CANCEL -> {
                subscription.setStatus(SubscriptionStatus.CANCELED);
                subscriptionRepository.save(subscription);
                log.info("Subscription {} canceled for user {} — access lapses at {}",
                        subscription.getExternalSubscriptionId(),
                        subscription.getUser().getId(),
                        subscription.getCurrentPeriodEnd());
            }
        }
    }
}

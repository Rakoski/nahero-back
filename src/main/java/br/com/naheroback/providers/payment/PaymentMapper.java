package br.com.naheroback.providers.payment;

import br.com.naheroback.modules.payment.useCases.processPayment.ProcessPaymentRequest;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
public class PaymentMapper {

    public Optional<ProcessPaymentRequest> mapStripe(Event event) {
        StripeObject object = deserialize(event);
        if (object == null) {
            return Optional.empty();
        }

        return switch (event.getType()) {
            case "checkout.session.completed" -> mapCheckoutCompleted((Session) object);
            case "invoice.paid" -> mapInvoicePaid((Invoice) object);
            case "customer.subscription.updated" -> mapSubscriptionUpdated((Subscription) object);
            case "customer.subscription.deleted" -> mapSubscriptionDeleted((Subscription) object);
            case "invoice.payment_failed" -> {
                log.info("Stripe invoice.payment_failed received for event {} — keeping access until period end",
                        event.getId());
                yield Optional.empty();
            }
            default -> {
                log.debug("Ignoring unhandled Stripe event type: {}", event.getType());
                yield Optional.empty();
            }
        };
    }

    // Stripe re-serializes webhook payloads to the endpoint's pinned API version,
    // but Event.list returns events in the account's *current* version. When the
    // account version differs from the Java SDK's pinned version, getObject() is
    // empty and we need deserializeUnsafe() as a fallback.
    private StripeObject deserialize(Event event) {
        StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
        if (object != null) {
            return object;
        }
        try {
            return event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (Exception e) {
            log.warn("Stripe event {} ({}) could not be deserialized: {}",
                    event.getId(), event.getType(), e.getMessage());
            return null;
        }
    }

    private Optional<ProcessPaymentRequest> mapCheckoutCompleted(Session session) {
        if (!"subscription".equals(session.getMode())) {
            return Optional.empty();
        }
        String customerId = session.getCustomer();
        String subscriptionId = session.getSubscription();
        if (customerId == null || subscriptionId == null) {
            return Optional.empty();
        }
        Instant periodEnd = fetchSubscriptionPeriodEnd(subscriptionId);
        return Optional.of(ProcessPaymentRequest.builder()
                .kind(ProcessPaymentRequest.Kind.ACTIVATE_OR_RENEW)
                .provider(PaymentProviderName.STRIPE)
                .externalCustomerId(customerId)
                .externalSubscriptionId(subscriptionId)
                .currentPeriodEnd(periodEnd)
                .build());
    }

    private Instant fetchSubscriptionPeriodEnd(String subscriptionId) {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            Long end = subscription.getCurrentPeriodEnd();
            if (end == null && subscription.getItems() != null
                    && subscription.getItems().getData() != null
                    && !subscription.getItems().getData().isEmpty()) {
                end = Subscription.retrieve(subscription.getItems().getData().getFirst().getSubscription()).getCurrentPeriodEnd();
            }
            return end != null ? Instant.ofEpochSecond(end) : null;
        } catch (StripeException e) {
            log.warn("Failed to retrieve subscription {} for period end: {}", subscriptionId, e.getMessage());
            return null;
        }
    }

    private Optional<ProcessPaymentRequest> mapInvoicePaid(Invoice invoice) {
        String customerId = invoice.getCustomer();
        String subscriptionId = invoice.getSubscription();
        if (customerId == null || subscriptionId == null) {
            return Optional.empty();
        }
        Long periodEndSeconds = invoice.getLines() != null && invoice.getLines().getData() != null
                && !invoice.getLines().getData().isEmpty()
                && invoice.getLines().getData().get(0).getPeriod() != null
                ? invoice.getLines().getData().get(0).getPeriod().getEnd()
                : null;

        Instant periodEnd = periodEndSeconds != null ? Instant.ofEpochSecond(periodEndSeconds) : null;
        return Optional.of(ProcessPaymentRequest.builder()
                .kind(ProcessPaymentRequest.Kind.ACTIVATE_OR_RENEW)
                .provider(PaymentProviderName.STRIPE)
                .externalCustomerId(customerId)
                .externalSubscriptionId(subscriptionId)
                .currentPeriodEnd(periodEnd)
                .build());
    }

    private Optional<ProcessPaymentRequest> mapSubscriptionDeleted(Subscription subscription) {
        String customerId = subscription.getCustomer();
        if (customerId == null) {
            return Optional.empty();
        }
        return Optional.of(ProcessPaymentRequest.builder()
                .kind(ProcessPaymentRequest.Kind.CANCEL)
                .provider(PaymentProviderName.STRIPE)
                .externalCustomerId(customerId)
                .externalSubscriptionId(subscription.getId())
                .currentPeriodEnd(null)
                .build());
    }

    // Fires when the subscription is modified — plan change, payment method, cancel_at_period_end toggled.
    // We only care about currentPeriodEnd + cancelAtPeriodEnd here; the subscription is still ACTIVE.
    private Optional<ProcessPaymentRequest> mapSubscriptionUpdated(Subscription subscription) {
        String customerId = subscription.getCustomer();
        if (customerId == null) {
            return Optional.empty();
        }
        Long end = subscription.getCurrentPeriodEnd();
        Instant periodEnd = end != null ? Instant.ofEpochSecond(end) : null;
        return Optional.of(ProcessPaymentRequest.builder()
                .kind(ProcessPaymentRequest.Kind.ACTIVATE_OR_RENEW)
                .provider(PaymentProviderName.STRIPE)
                .externalCustomerId(customerId)
                .externalSubscriptionId(subscription.getId())
                .currentPeriodEnd(periodEnd)
                .cancelAtPeriodEnd(Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd()))
                .build());
    }
}

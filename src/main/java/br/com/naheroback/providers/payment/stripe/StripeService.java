package br.com.naheroback.providers.payment.stripe;

import br.com.naheroback.common.exceptions.custom.UnprocessableEntityException;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.providers.payment.CheckoutSessionResult;
import br.com.naheroback.providers.payment.PaymentProvider;
import br.com.naheroback.providers.payment.PlanInterval;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService implements PaymentProvider {

    private final StripeConfig stripeConfig;

    @Override
    public String ensureCustomer(User user) {
        if (user.getExternalCustomerId() != null && !user.getExternalCustomerId().isBlank()) {
            return user.getExternalCustomerId();
        }
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(user.getEmail())
                    .setName(user.getName())
                    .putMetadata("nahero_user_id", String.valueOf(user.getId()))
                    .build();
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey("customer-user-" + user.getId())
                    .build();
            Customer customer = Customer.create(params, options);
            return customer.getId();
        } catch (StripeException e) {
            log.error("Failed to create Stripe customer for user {}: {}", user.getId(), e.getMessage());
            throw new UnprocessableEntityException("payment.stripe.customer_create_failed");
        }
    }

    @Override
    public CheckoutSessionResult createSubscriptionCheckout(
            User user,
            PlanInterval plan,
            String successUrl,
            String cancelUrl
    ) {
        String customerId = user.getExternalCustomerId();
        if (customerId == null || customerId.isBlank()) throw new UnprocessableEntityException("payment.stripe.customer_missing");

        String priceId = resolvePriceId(plan);
        String resolvedSuccess = successUrl != null ? successUrl : stripeConfig.getSuccessUrl();
        String resolvedCancel = cancelUrl != null ? cancelUrl : stripeConfig.getCancelUrl();

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .setSuccessUrl(resolvedSuccess)
                    .setCancelUrl(resolvedCancel)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(priceId)
                                    .setQuantity(1L)
                                    .build()
                    )
                    .putMetadata("nahero_user_id", String.valueOf(user.getId()))
                    .putMetadata("nahero_plan", plan.name())
                    .build();

            Session session = Session.create(params);
            return new CheckoutSessionResult(session.getId(), session.getUrl());
        } catch (StripeException e) {
            log.error("Failed to create Stripe checkout session for user {} plan {}: {}",
                    user.getId(), plan, e.getMessage());
            throw new UnprocessableEntityException("payment.stripe.checkout_create_failed");
        }
    }

    @Override
    public void cancelSubscriptionAtPeriodEnd(String externalSubscriptionId) {
        try {
            Subscription subscription = Subscription.retrieve(externalSubscriptionId);
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(true)
                    .build();
            subscription.update(params);
        } catch (StripeException e) {
            log.error("Failed to cancel Stripe subscription {} at period end: {}",
                    externalSubscriptionId, e.getMessage());
            throw new UnprocessableEntityException("payment.stripe.cancel_failed");
        }
    }

    private String resolvePriceId(PlanInterval plan) {
        return switch (plan) {
            case MONTHLY -> stripeConfig.getPriceMonthly();
            case YEARLY -> stripeConfig.getPriceYearly();
        };
    }
}

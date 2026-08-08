package br.com.naheroback.providers.payment;

import br.com.naheroback.modules.user.entities.User;

public interface PaymentProvider {

    String ensureCustomer(User user);

    CheckoutSessionResult createSubscriptionCheckout(
            User user,
            PlanInterval plan,
            String successUrl,
            String cancelUrl
    );

    void cancelSubscriptionAtPeriodEnd(String externalSubscriptionId);
}

package br.com.naheroback.modules.payment.useCases.createCheckoutSession;

import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import br.com.naheroback.providers.payment.CheckoutSessionResult;
import br.com.naheroback.providers.payment.PaymentProvider;
import br.com.naheroback.providers.payment.PaymentProviderName;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateCheckoutSessionUseCase {

    private final PaymentProvider paymentProvider;
    private final UserRepository userRepository;

    @Transactional
    public CreateCheckoutSessionResponse execute(CreateCheckoutSessionRequest request) {
        User user = AuthService.getUserFromToken();

        if (user.getExternalCustomerId() == null || user.getExternalCustomerId().isBlank()) {
            String customerId = paymentProvider.ensureCustomer(user);
            user.setExternalCustomerId(customerId);
            user.setPaymentProvider(PaymentProviderName.STRIPE);
            user = userRepository.save(user);
        }

        CheckoutSessionResult result = paymentProvider.createSubscriptionCheckout(
                user,
                request.plan(),
                request.successUrl(),
                request.cancelUrl()
        );

        return new CreateCheckoutSessionResponse(result.sessionId(), result.checkoutUrl());
    }
}

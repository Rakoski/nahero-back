package br.com.naheroback.providers.payment.stripe;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class StripeWebhookVerifier {

    private final StripeConfig stripeConfig;

    public Event verifyAndParse(String rawPayload, String signatureHeader) {
        try {
            return Webhook.constructEvent(rawPayload, signatureHeader, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payment.stripe.signature_invalid");
        }
    }
}

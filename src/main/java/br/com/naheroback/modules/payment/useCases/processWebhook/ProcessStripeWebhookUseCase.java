package br.com.naheroback.modules.payment.useCases.processWebhook;

import br.com.naheroback.modules.payment.useCases.processStripeEvent.ProcessStripeEventUseCase;
import br.com.naheroback.providers.payment.stripe.StripeWebhookVerifier;
import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessStripeWebhookUseCase {

    private final StripeWebhookVerifier webhookVerifier;
    private final ProcessStripeEventUseCase processStripeEventUseCase;

    public void execute(String rawPayload, String signatureHeader) {
        Event event = webhookVerifier.verifyAndParse(rawPayload, signatureHeader);
        processStripeEventUseCase.execute(event);
    }
}

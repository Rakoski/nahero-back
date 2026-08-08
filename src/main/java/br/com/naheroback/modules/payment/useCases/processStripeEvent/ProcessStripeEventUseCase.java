package br.com.naheroback.modules.payment.useCases.processStripeEvent;

import br.com.naheroback.modules.payment.entities.EventProcessingStatus;
import br.com.naheroback.modules.payment.entities.ProcessedPaymentEvent;
import br.com.naheroback.modules.payment.repositories.ProcessedPaymentEventRepository;
import br.com.naheroback.modules.payment.useCases.processPayment.ProcessPaymentRequest;
import br.com.naheroback.modules.payment.useCases.processPayment.ProcessPaymentUseCase;
import br.com.naheroback.providers.payment.PaymentMapper;
import br.com.naheroback.providers.payment.PaymentProviderName;
import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessStripeEventUseCase {

    private final PaymentMapper paymentMapper;
    private final ProcessPaymentUseCase processPaymentUseCase;
    private final ProcessedPaymentEventRepository processedPaymentEventRepository;

    @Value("${payment.reconcile.max-attempts:5}")
    private int maxAttempts;

    public Outcome execute(Event event) {
        Optional<ProcessedPaymentEvent> existingOpt = processedPaymentEventRepository.findById(event.getId());

        if (existingOpt.isPresent()) {
            EventProcessingStatus existingStatus = existingOpt.get().getStatus();
            if (existingStatus != EventProcessingStatus.DEFERRED) {
                // APPLIED, IGNORED, or FAILED — nothing more to do.
                log.debug("Stripe event {} ({}) already in terminal status {} — skipping",
                        event.getId(), event.getType(), existingStatus);
                return Outcome.ALREADY_SEEN;
            }
            // Fall through — DEFERRED means we can retry.
        }

        EventProcessingStatus outcome = processOnce(event);

        ProcessedPaymentEvent record = existingOpt.orElseGet(() -> {
            ProcessedPaymentEvent fresh = new ProcessedPaymentEvent();
            fresh.setEventId(event.getId());
            fresh.setProvider(PaymentProviderName.STRIPE);
            fresh.setType(event.getType());
            fresh.setAttempts(0);
            return fresh;
        });

        record.setReceivedAt(Instant.now());
        record.setAttempts(record.getAttempts() + 1);

        if (outcome == EventProcessingStatus.DEFERRED && record.getAttempts() >= maxAttempts) {
            record.setStatus(EventProcessingStatus.FAILED);
            log.error("Stripe event {} ({}) permanently failed after {} attempts — needs human review",
                    event.getId(), event.getType(), record.getAttempts());
            processedPaymentEventRepository.save(record);
            return Outcome.FAILED_PERMANENTLY;
        }

        record.setStatus(outcome);
        processedPaymentEventRepository.save(record);

        return switch (outcome) {
            case APPLIED -> Outcome.APPLIED;
            case IGNORED -> Outcome.IGNORED;
            case DEFERRED -> Outcome.DEFERRED;
            case FAILED -> Outcome.FAILED_PERMANENTLY;
        };
    }

    private EventProcessingStatus processOnce(Event event) {
        Optional<ProcessPaymentRequest> request = paymentMapper.mapStripe(event);
        if (request.isEmpty()) {
            return EventProcessingStatus.IGNORED;
        }
        return processPaymentUseCase.execute(request.get());
    }

    public enum Outcome {
        ALREADY_SEEN,
        APPLIED,
        IGNORED,
        DEFERRED,
        FAILED_PERMANENTLY
    }
}

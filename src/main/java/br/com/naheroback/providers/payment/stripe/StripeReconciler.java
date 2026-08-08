package br.com.naheroback.providers.payment.stripe;

import br.com.naheroback.modules.payment.useCases.processStripeEvent.ProcessStripeEventUseCase;
import br.com.naheroback.providers.payment.PaymentProviderName;
import br.com.naheroback.providers.payment.PaymentReconciler;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventCollection;
import com.stripe.param.EventListParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StripeReconciler implements PaymentReconciler {

    private static final int PAGE_SIZE = 100;

    // Keep in sync with PaymentMapper.mapStripe — no point fetching events we ignore.
    private static final List<String> RECONCILED_EVENT_TYPES = List.of(
            "checkout.session.completed",
            "invoice.paid",
            "customer.subscription.updated",
            "customer.subscription.deleted",
            "invoice.payment_failed"
    );

    private final ProcessStripeEventUseCase processStripeEventUseCase;

    @Override
    public PaymentProviderName provider() {
        return PaymentProviderName.STRIPE;
    }

    @Override
    public ReconcileResult reconcile(int lookbackHours) {
        long createdGte = Instant.now().minus(Duration.ofHours(lookbackHours)).getEpochSecond();

        int fetched = 0;
        int applied = 0;
        int alreadySeen = 0;
        int ignored = 0;
        int deferred = 0;
        int permanentlyFailed = 0;
        int errors = 0;

        String startingAfter = null;
        try {
            while (true) {
                EventListParams.Builder builder = EventListParams.builder()
                        .setLimit((long) PAGE_SIZE)
                        .setCreated(EventListParams.Created.builder().setGte(createdGte).build())
                        .addAllType(RECONCILED_EVENT_TYPES);

                if (startingAfter != null) builder.setStartingAfter(startingAfter);

                EventCollection page = Event.list(builder.build());
                if (page.getData() == null || page.getData().isEmpty()) break;

                for (Event event : page.getData()) {
                    fetched++;
                    try {
                        ProcessStripeEventUseCase.Outcome outcome = processStripeEventUseCase.execute(event);
                        switch (outcome) {
                            case APPLIED -> applied++;
                            case ALREADY_SEEN -> alreadySeen++;
                            case IGNORED -> ignored++;
                            case DEFERRED -> deferred++;
                            case FAILED_PERMANENTLY -> permanentlyFailed++;
                        }
                    } catch (Exception e) {
                        errors++;
                        log.warn("Stripe reconcile: failed to process event {} ({}): {}",
                                event.getId(), event.getType(), e.getMessage());
                    }
                }

                if (Boolean.FALSE.equals(page.getHasMore())) break;
                startingAfter = page.getData().getLast().getId();
            }
        } catch (StripeException e) {
            errors++;
            log.error("Stripe reconcile: API call failed: {}", e.getMessage());
        }

        return new ReconcileResult(fetched, applied, alreadySeen, ignored, deferred, permanentlyFailed, errors);
    }
}

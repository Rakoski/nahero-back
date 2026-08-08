package br.com.naheroback.modules.payment.useCases.reconcilePaymentEvents;

import br.com.naheroback.providers.payment.PaymentReconciler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconcilePaymentEventsUseCase {

    private final List<PaymentReconciler> reconcilers;

    @Value("${payment.reconcile.lookback-hours:24}")
    private int lookbackHours;

    @Value("${payment.reconcile.enabled:true}")
    private boolean scheduledEnabled;

    @Scheduled(
            fixedDelayString = "${payment.reconcile.fixed-delay-ms:900000}",
            initialDelayString = "${payment.reconcile.initial-delay-ms:60000}"
    )
    public void runScheduled() {
        if (!scheduledEnabled) {
            return;
        }
        ReconcilePaymentEventsResponse summary = execute();
        for (ReconcilePaymentEventsResponse.ProviderReconcileEntry entry : summary.providers()) {
            log.info("Payment reconciliation (scheduled) [{}]: fetched={} applied={} alreadySeen={} ignored={} deferred={} permanentlyFailed={} errors={}",
                    entry.provider(), entry.fetched(), entry.applied(), entry.alreadySeen(),
                    entry.ignored(), entry.deferred(), entry.permanentlyFailed(), entry.errors());
        }
    }

    public ReconcilePaymentEventsResponse execute() {
        List<ReconcilePaymentEventsResponse.ProviderReconcileEntry> results = reconcilers.stream()
                .map(reconciler -> {
                    PaymentReconciler.ReconcileResult result = reconciler.reconcile(lookbackHours);
                    return new ReconcilePaymentEventsResponse.ProviderReconcileEntry(
                            reconciler.provider(),
                            result.fetched(), result.applied(), result.alreadySeen(),
                            result.ignored(), result.deferred(), result.permanentlyFailed(), result.errors()
                    );
                })
                .toList();
        return new ReconcilePaymentEventsResponse(Duration.ofHours(lookbackHours).getSeconds(), results);
    }
}

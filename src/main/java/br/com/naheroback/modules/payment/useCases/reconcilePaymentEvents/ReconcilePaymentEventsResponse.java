package br.com.naheroback.modules.payment.useCases.reconcilePaymentEvents;

import br.com.naheroback.providers.payment.PaymentProviderName;

import java.util.List;

public record ReconcilePaymentEventsResponse(
        long lookbackSeconds,
        List<ProviderReconcileEntry> providers
) {

    public record ProviderReconcileEntry(
            PaymentProviderName provider,
            int fetched,
            int applied,
            int alreadySeen,
            int ignored,
            int deferred,
            int permanentlyFailed,
            int errors
    ) {
    }
}

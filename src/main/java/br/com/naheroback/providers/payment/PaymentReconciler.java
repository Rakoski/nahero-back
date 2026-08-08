package br.com.naheroback.providers.payment;

public interface PaymentReconciler {

    PaymentProviderName provider();

    ReconcileResult reconcile(int lookbackHours);

    record ReconcileResult(
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

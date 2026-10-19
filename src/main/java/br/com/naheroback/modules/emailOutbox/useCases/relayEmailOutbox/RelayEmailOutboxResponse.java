package br.com.naheroback.modules.emailOutbox.useCases.relayEmailOutbox;

public record RelayEmailOutboxResponse(
        int sent,
        int retrying,
        int failed,
        int skipped
) {

    public int total() {
        return sent + retrying + failed + skipped;
    }
}

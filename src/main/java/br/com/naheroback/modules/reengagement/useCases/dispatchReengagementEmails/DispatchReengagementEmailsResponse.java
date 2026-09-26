package br.com.naheroback.modules.reengagement.useCases.dispatchReengagementEmails;

public record DispatchReengagementEmailsResponse(
        int candidates,
        int sent,
        int failed,
        int skipped
) {
}

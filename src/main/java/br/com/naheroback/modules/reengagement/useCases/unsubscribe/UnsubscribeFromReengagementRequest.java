package br.com.naheroback.modules.reengagement.useCases.unsubscribe;

import jakarta.validation.constraints.NotBlank;

public record UnsubscribeFromReengagementRequest(
        @NotBlank(message = "{reengagement.token.required}") String token
) {
}

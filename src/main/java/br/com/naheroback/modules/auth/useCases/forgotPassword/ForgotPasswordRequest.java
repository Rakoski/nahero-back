package br.com.naheroback.modules.auth.useCases.forgotPassword;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @Email(message = "{auth.email.invalid}")
        @NotBlank(message = "{auth.email.required}")
        String email
) {
}

package br.com.naheroback.modules.user.useCases.user.resendEmailVerification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendEmailVerificationRequest(
        @Email(message = "{auth.email.invalid}")
        @NotBlank(message = "{auth.email.required}")
        String email
) {
}

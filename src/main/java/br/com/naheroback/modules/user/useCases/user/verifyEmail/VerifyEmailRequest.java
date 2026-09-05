package br.com.naheroback.modules.user.useCases.user.verifyEmail;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
        @NotBlank(message = "{auth.verificationtoken.required}") String verificationToken
) {
}

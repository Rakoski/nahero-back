package br.com.naheroback.modules.auth.useCases.resetPassword;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "{auth.resettoken.required}") String resetToken,
        @NotBlank(message = "{auth.password.required}")
        @Size(min = 6, message = "{auth.password.min}") String password,
        @NotBlank(message = "{auth.confirmpassword.required}") String confirmPassword
) {
}

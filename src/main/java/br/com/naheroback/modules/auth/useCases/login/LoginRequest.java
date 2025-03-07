package br.com.naheroback.modules.auth.useCases.login;

import br.com.naheroback.modules.user.entities.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @Email(message = "Email is invalid")
    @NotBlank(message = "Email is required")
    String email,

    @NotBlank(message = "Password is required")
    String password
) {
    public static User toDomain(LoginRequest input) {
        final var user = new User();
        user.setEmail(input.email);
        user.setPassword(input.password);
        return user;
    }
}


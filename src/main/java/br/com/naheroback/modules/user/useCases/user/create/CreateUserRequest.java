package br.com.naheroback.modules.user.useCases.user.create;

import br.com.naheroback.modules.user.entities.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank(message = "{user.name.required}") String name,
        @Email(message = "{user.email.invalid}") @NotBlank(message = "{user.email.required}") String email,
        @NotBlank(message = "{user.password.required}") String password
) {

    public static User toDomain(CreateUserRequest input) {
        final var user = new User();
        user.setName(input.name);
        user.setEmail(input.email);
        user.setPassword(input.password);
        return user;
    }
}
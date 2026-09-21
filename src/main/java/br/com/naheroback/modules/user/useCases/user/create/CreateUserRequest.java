package br.com.naheroback.modules.user.useCases.user.create;

import br.com.naheroback.modules.user.entities.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "{user.name.required}") String name,
        @Email(message = "{user.email.invalid}") @NotBlank(message = "{user.email.required}") String email,
        @NotBlank(message = "{user.password.required}") String password,
        Utm utm
) {

    private static final int UTM_MAX_LENGTH = 64;

    public record Utm(
            @Size(max = UTM_MAX_LENGTH) String source,
            @Size(max = UTM_MAX_LENGTH) String medium,
            @Size(max = UTM_MAX_LENGTH) String campaign
    ) {
    }

    public static User toDomain(CreateUserRequest input) {
        final var user = new User();
        user.setName(input.name);
        user.setEmail(input.email);
        user.setPassword(input.password);

        if (input.utm != null) {
            user.setUtmSource(clamp(input.utm.source()));
            user.setUtmMedium(clamp(input.utm.medium()));
            user.setUtmCampaign(clamp(input.utm.campaign()));
        }

        return user;
    }

    private static String clamp(String value) {
        if (value == null || value.isBlank()) return null;

        final var trimmed = value.trim();
        return trimmed.length() <= UTM_MAX_LENGTH ? trimmed : trimmed.substring(0, UTM_MAX_LENGTH);
    }
}

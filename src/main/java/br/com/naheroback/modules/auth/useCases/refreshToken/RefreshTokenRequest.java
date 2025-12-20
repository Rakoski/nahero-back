package br.com.naheroback.modules.auth.useCases.refreshToken;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank(message = "{auth.refreshtoken.required}") String refreshToken
) {
    public static RefreshTokenRequest toDomain(RefreshTokenRequest input) {
        return new RefreshTokenRequest(input.refreshToken);
    }
}

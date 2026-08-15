package br.com.naheroback.modules.auth.controllers;

import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.auth.useCases.forgotPassword.ForgotPasswordRequest;
import br.com.naheroback.modules.auth.useCases.forgotPassword.ForgotPasswordUseCase;
import br.com.naheroback.modules.auth.useCases.login.LoginRequest;
import br.com.naheroback.modules.auth.useCases.login.LoginResponse;
import br.com.naheroback.modules.auth.useCases.refreshToken.RefreshTokenRequest;
import br.com.naheroback.modules.auth.useCases.refreshToken.RefreshTokenResponse;
import br.com.naheroback.modules.auth.useCases.resetPassword.ResetPasswordRequest;
import br.com.naheroback.modules.auth.useCases.resetPassword.ResetPasswordUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest);
    }

    @PostMapping("/refresh-token")
    @ResponseStatus(HttpStatus.OK)
    public RefreshTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        return authService.refresh(refreshTokenRequest);
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        forgotPasswordUseCase.execute(forgotPasswordRequest);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        resetPasswordUseCase.execute(resetPasswordRequest);
    }
}

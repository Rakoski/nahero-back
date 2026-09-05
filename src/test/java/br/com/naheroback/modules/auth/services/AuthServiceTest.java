package br.com.naheroback.modules.auth.services;

import br.com.naheroback.common.exceptions.custom.EmailNotVerifiedException;
import br.com.naheroback.common.exceptions.custom.UnauthorizedException;
import br.com.naheroback.modules.auth.entities.AuthenticatedUser;
import br.com.naheroback.modules.auth.useCases.login.LoginRequest;
import br.com.naheroback.modules.auth.useCases.login.LoginResponse;
import br.com.naheroback.modules.auth.useCases.refreshToken.RefreshTokenResponse;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "s3cret";

    @Mock
    private LoginResponse loginUserResponse;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenResponse refreshTokenResponse;

    @InjectMocks
    private AuthService authService;

    private User user;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setEmail(EMAIL);
        user.setPassword(PASSWORD);

        loginRequest = new LoginRequest(EMAIL, PASSWORD);
    }

    private void givenCredentialsAccepted() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(new AuthenticatedUser(user), null);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
    }

    @Test
    @DisplayName("Should refuse to issue tokens while the email is unconfirmed")
    void shouldRefuseLoginWhenEmailUnconfirmed() {
        givenCredentialsAccepted();

        assertThrows(EmailNotVerifiedException.class, () -> authService.login(loginRequest));

        verify(jwtService, never()).generateToken(any(), any(), any(), anyString());
    }

    @Test
    @DisplayName("Should issue tokens once the email is confirmed")
    void shouldIssueTokensWhenEmailConfirmed() {
        user.setEmailConfirmedAt(LocalDateTime.now());
        givenCredentialsAccepted();

        assertDoesNotThrow(() -> authService.login(loginRequest));

        verify(jwtService, times(2)).generateToken(any(), any(), any(), anyString());
    }

    @Test
    @DisplayName("Should report bad credentials as unauthorized, not as an unverified email")
    void shouldReportBadCredentialsAsUnauthorized() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("nope"));

        assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest));
    }
}

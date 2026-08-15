package br.com.naheroback.modules.auth.useCases.resetPassword;

import br.com.naheroback.common.exceptions.custom.UnauthorizedException;
import br.com.naheroback.common.exceptions.custom.UnprocessableEntityException;
import br.com.naheroback.common.utils.TokenHasher;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResetPasswordUseCaseTest {

    private static final String RAW_TOKEN = "raw-reset-token";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ResetPasswordUseCase resetPasswordUseCase;

    private ResetPasswordRequest validRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        validRequest = new ResetPasswordRequest(RAW_TOKEN, "newPassword123", "newPassword123");

        mockUser = new User();
        mockUser.setId(1);
        mockUser.setEmail("test@example.com");
        mockUser.setPassword("oldEncodedPassword");
        mockUser.setForgotPasswordToken(TokenHasher.hash(RAW_TOKEN));
        mockUser.setForgotPasswordTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
    }

    @Test
    @DisplayName("Should reset the password and clear the token")
    void shouldResetPasswordAndClearToken() {
        when(userRepository.findByForgotPasswordToken(TokenHasher.hash(RAW_TOKEN)))
                .thenReturn(Optional.of(mockUser));
        when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");

        resetPasswordUseCase.execute(validRequest);

        assertEquals("newEncodedPassword", mockUser.getPassword());
        assertNull(mockUser.getForgotPasswordToken());
        assertNull(mockUser.getForgotPasswordTokenExpiresAt());

        verify(passwordEncoder, times(1)).encode(validRequest.password());
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("Should throw UnprocessableEntityException when passwords do not match")
    void shouldThrowWhenPasswordsDoNotMatch() {
        var mismatchedRequest = new ResetPasswordRequest(RAW_TOKEN, "newPassword123", "differentPassword");

        UnprocessableEntityException exception = assertThrows(
                UnprocessableEntityException.class,
                () -> resetPasswordUseCase.execute(mismatchedRequest)
        );

        assertEquals("auth.password.mismatch", exception.getMessageKey());

        verify(userRepository, never()).findByForgotPasswordToken(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when the token is unknown")
    void shouldThrowWhenTokenIsUnknown() {
        when(userRepository.findByForgotPasswordToken(anyString())).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> resetPasswordUseCase.execute(validRequest));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when the token is expired")
    void shouldThrowWhenTokenIsExpired() {
        mockUser.setForgotPasswordTokenExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByForgotPasswordToken(anyString())).thenReturn(Optional.of(mockUser));

        assertThrows(UnauthorizedException.class, () -> resetPasswordUseCase.execute(validRequest));

        assertEquals("oldEncodedPassword", mockUser.getPassword());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when the token has no expiration")
    void shouldThrowWhenTokenHasNoExpiration() {
        mockUser.setForgotPasswordTokenExpiresAt(null);
        when(userRepository.findByForgotPasswordToken(anyString())).thenReturn(Optional.of(mockUser));

        assertThrows(UnauthorizedException.class, () -> resetPasswordUseCase.execute(validRequest));

        verify(userRepository, never()).save(any(User.class));
    }
}

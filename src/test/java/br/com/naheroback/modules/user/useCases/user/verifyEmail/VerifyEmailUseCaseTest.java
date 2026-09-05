package br.com.naheroback.modules.user.useCases.user.verifyEmail;

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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerifyEmailUseCaseTest {

    private static final String RAW_TOKEN = "raw-verification-token";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VerifyEmailUseCase verifyEmailUseCase;

    private VerifyEmailRequest validRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        validRequest = new VerifyEmailRequest(RAW_TOKEN);

        mockUser = new User();
        mockUser.setId(1);
        mockUser.setName("Test User");
        mockUser.setEmail("test@example.com");
        mockUser.setEmailVerificationToken(TokenHasher.hash(RAW_TOKEN));
        mockUser.setEmailVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
    }

    @Test
    @DisplayName("Should look the user up by the hashed token")
    void shouldLookUserUpByHashedToken() {
        when(userRepository.findByEmailVerificationToken(anyString())).thenReturn(Optional.of(mockUser));

        verifyEmailUseCase.execute(validRequest);

        verify(userRepository, times(1)).findByEmailVerificationToken(TokenHasher.hash(RAW_TOKEN));
    }

    @Test
    @DisplayName("Should confirm the email and leave the token to expire on its own")
    void shouldConfirmEmailAndKeepToken() {
        when(userRepository.findByEmailVerificationToken(anyString())).thenReturn(Optional.of(mockUser));

        verifyEmailUseCase.execute(validRequest);

        assertNotNull(mockUser.getEmailConfirmedAt());
        assertTrue(mockUser.isEmailConfirmed());
        assertEquals(TokenHasher.hash(RAW_TOKEN), mockUser.getEmailVerificationToken());
        assertNotNull(mockUser.getEmailVerificationTokenExpiresAt());

        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("Should accept a token that was already used, so link scanners cannot break the flow")
    void shouldAcceptAlreadyUsedToken() {
        LocalDateTime confirmedAt = LocalDateTime.now().minusMinutes(5);
        mockUser.setEmailConfirmedAt(confirmedAt);
        when(userRepository.findByEmailVerificationToken(anyString())).thenReturn(Optional.of(mockUser));

        assertDoesNotThrow(() -> verifyEmailUseCase.execute(validRequest));

        assertEquals(confirmedAt, mockUser.getEmailConfirmedAt());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should still reject an expired token for an unconfirmed user")
    void shouldRejectExpiredTokenEvenWhenIdempotent() {
        mockUser.setEmailVerificationTokenExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(userRepository.findByEmailVerificationToken(anyString())).thenReturn(Optional.of(mockUser));

        assertThrows(UnprocessableEntityException.class, () -> verifyEmailUseCase.execute(validRequest));

        assertNull(mockUser.getEmailConfirmedAt());
    }

    @Test
    @DisplayName("Should reject an unknown token")
    void shouldRejectUnknownToken() {
        when(userRepository.findByEmailVerificationToken(anyString())).thenReturn(Optional.empty());

        UnprocessableEntityException exception = assertThrows(
                UnprocessableEntityException.class,
                () -> verifyEmailUseCase.execute(validRequest)
        );

        assertEquals("auth.email_verification.invalid_token", exception.getMessageKey());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject an expired token")
    void shouldRejectExpiredToken() {
        mockUser.setEmailVerificationTokenExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmailVerificationToken(anyString())).thenReturn(Optional.of(mockUser));

        UnprocessableEntityException exception = assertThrows(
                UnprocessableEntityException.class,
                () -> verifyEmailUseCase.execute(validRequest)
        );

        assertEquals("auth.email_verification.invalid_token", exception.getMessageKey());
        assertNull(mockUser.getEmailConfirmedAt());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject a token without an expiration date")
    void shouldRejectTokenWithoutExpirationDate() {
        mockUser.setEmailVerificationTokenExpiresAt(null);
        when(userRepository.findByEmailVerificationToken(anyString())).thenReturn(Optional.of(mockUser));

        assertThrows(UnprocessableEntityException.class, () -> verifyEmailUseCase.execute(validRequest));

        verify(userRepository, never()).save(any(User.class));
    }
}

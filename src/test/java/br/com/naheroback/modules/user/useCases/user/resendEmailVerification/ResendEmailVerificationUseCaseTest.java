package br.com.naheroback.modules.user.useCases.user.resendEmailVerification;

import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import br.com.naheroback.modules.user.services.EmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResendEmailVerificationUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private ResendEmailVerificationUseCase resendEmailVerificationUseCase;

    private ResendEmailVerificationRequest validRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        validRequest = new ResendEmailVerificationRequest("test@example.com");

        mockUser = new User();
        mockUser.setId(1);
        mockUser.setName("Test User");
        mockUser.setEmail("test@example.com");
    }

    @Test
    @DisplayName("Should issue a new verification email for an unconfirmed user")
    void shouldIssueNewVerificationEmailForUnconfirmedUser() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));

        resendEmailVerificationUseCase.execute(validRequest);

        verify(emailVerificationService, times(1)).issueAndSend(mockUser);
    }

    @Test
    @DisplayName("Should not resend when the email is already confirmed")
    void shouldNotResendWhenEmailIsAlreadyConfirmed() {
        mockUser.setEmailConfirmedAt(LocalDateTime.now());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));

        resendEmailVerificationUseCase.execute(validRequest);

        verify(emailVerificationService, never()).issueAndSend(any(User.class));
    }

    @Test
    @DisplayName("Should not leak that the email is unknown")
    void shouldNotLeakThatEmailIsUnknown() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> resendEmailVerificationUseCase.execute(validRequest));

        verify(emailVerificationService, never()).issueAndSend(any(User.class));
    }
}

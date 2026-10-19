package br.com.naheroback.modules.auth.useCases.forgotPassword;

import br.com.naheroback.modules.emailOutbox.entities.enums.EmailOutboxType;
import br.com.naheroback.modules.emailOutbox.services.EmailOutboxService;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailOutboxService emailOutboxService;

    @InjectMocks
    private ForgotPasswordUseCase forgotPasswordUseCase;

    private ForgotPasswordRequest validRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        validRequest = new ForgotPasswordRequest("test@example.com");

        mockUser = new User();
        mockUser.setId(1);
        mockUser.setName("Test User");
        mockUser.setEmail("test@example.com");
    }

    @Test
    @DisplayName("Should queue the password reset email for a known user")
    void shouldQueuePasswordResetForKnownUser() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));

        forgotPasswordUseCase.execute(validRequest);

        verify(emailOutboxService, times(1)).enqueue(mockUser, EmailOutboxType.PASSWORD_RESET);
    }

    @Test
    @DisplayName("Should not issue a token while the email is only queued")
    void shouldNotIssueTokenWhileQueued() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));

        forgotPasswordUseCase.execute(validRequest);

        assertNull(mockUser.getForgotPasswordToken());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should not leak that the email is unknown")
    void shouldNotLeakThatEmailIsUnknown() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> forgotPasswordUseCase.execute(validRequest));

        verify(emailOutboxService, never()).enqueue(any(User.class), any(EmailOutboxType.class));
    }
}

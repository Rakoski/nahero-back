package br.com.naheroback.modules.auth.useCases.forgotPassword;

import br.com.naheroback.common.services.EmailService;
import br.com.naheroback.common.utils.TokenHasher;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
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
    private EmailService emailService;

    @InjectMocks
    private ForgotPasswordUseCase forgotPasswordUseCase;

    private ForgotPasswordRequest validRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(forgotPasswordUseCase, "expirationMinutes", 60);

        validRequest = new ForgotPasswordRequest("test@example.com");

        mockUser = new User();
        mockUser.setId(1);
        mockUser.setName("Test User");
        mockUser.setEmail("test@example.com");
    }

    @Test
    @DisplayName("Should store the hashed token and email the raw one")
    void shouldStoreHashedTokenAndEmailRawToken() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));

        forgotPasswordUseCase.execute(validRequest);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(1))
                .sendPasswordResetEmail(eq(mockUser.getEmail()), eq(mockUser.getName()), tokenCaptor.capture());

        String emailedToken = tokenCaptor.getValue();

        assertNotNull(emailedToken);
        assertNotEquals(emailedToken, mockUser.getForgotPasswordToken());
        assertEquals(TokenHasher.hash(emailedToken), mockUser.getForgotPasswordToken());

        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("Should set the token expiration in the future")
    void shouldSetTokenExpirationInTheFuture() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));

        forgotPasswordUseCase.execute(validRequest);

        assertNotNull(mockUser.getForgotPasswordTokenExpiresAt());
        assertTrue(mockUser.getForgotPasswordTokenExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should not leak that the email is unknown")
    void shouldNotLeakThatEmailIsUnknown() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> forgotPasswordUseCase.execute(validRequest));

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should generate a different token on every request")
    void shouldGenerateDifferentTokenOnEveryRequest() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));

        forgotPasswordUseCase.execute(validRequest);
        String firstToken = mockUser.getForgotPasswordToken();

        forgotPasswordUseCase.execute(validRequest);
        String secondToken = mockUser.getForgotPasswordToken();

        assertNotEquals(firstToken, secondToken);
    }
}

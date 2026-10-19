package br.com.naheroback.modules.auth.services;

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
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final Locale PORTUGUESE = Locale.forLanguageTag("pt-BR");

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "expirationMinutes", 60);

        mockUser = new User();
        mockUser.setId(1);
        mockUser.setName("Test User");
        mockUser.setEmail("test@example.com");
    }

    @Test
    @DisplayName("Should store the hashed token and email the raw one in the queued locale")
    void shouldStoreHashedTokenAndEmailRawToken() {
        passwordResetService.issueAndSend(mockUser, PORTUGUESE);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(1))
                .sendPasswordResetEmail(eq(mockUser.getEmail()), eq(mockUser.getName()), tokenCaptor.capture(), eq(PORTUGUESE));

        String emailedToken = tokenCaptor.getValue();

        assertNotNull(emailedToken);
        assertNotEquals(emailedToken, mockUser.getForgotPasswordToken());
        assertEquals(TokenHasher.hash(emailedToken), mockUser.getForgotPasswordToken());

        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("Should set the token expiration in the future")
    void shouldSetTokenExpirationInTheFuture() {
        passwordResetService.issueAndSend(mockUser, PORTUGUESE);

        assertNotNull(mockUser.getForgotPasswordTokenExpiresAt());
        assertTrue(mockUser.getForgotPasswordTokenExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should generate a different token on every issue")
    void shouldGenerateDifferentTokenOnEveryIssue() {
        passwordResetService.issueAndSend(mockUser, PORTUGUESE);
        String firstToken = mockUser.getForgotPasswordToken();

        passwordResetService.issueAndSend(mockUser, PORTUGUESE);
        String secondToken = mockUser.getForgotPasswordToken();

        assertNotEquals(firstToken, secondToken);
    }

    @Test
    @DisplayName("Should keep the previous token when sending fails")
    void shouldKeepPreviousTokenWhenSendingFails() {
        mockUser.setForgotPasswordToken("previous-hash");
        doThrow(new IllegalStateException("smtp is down"))
                .when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString(), any(Locale.class));

        assertThrows(IllegalStateException.class, () -> passwordResetService.issueAndSend(mockUser, PORTUGUESE));

        assertEquals("previous-hash", mockUser.getForgotPasswordToken());
        verify(userRepository, never()).save(any(User.class));
    }
}

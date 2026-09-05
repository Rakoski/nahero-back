package br.com.naheroback.modules.user.services;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailVerificationService, "expirationHours", 24);

        mockUser = new User();
        mockUser.setId(1);
        mockUser.setName("Test User");
        mockUser.setEmail("test@example.com");
    }

    @Test
    @DisplayName("Should store the hashed token and email the raw one")
    void shouldStoreHashedTokenAndEmailRawToken() {
        emailVerificationService.issueAndSend(mockUser);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(1))
                .sendEmailVerificationEmail(eq(mockUser.getEmail()), eq(mockUser.getName()), tokenCaptor.capture());

        String emailedToken = tokenCaptor.getValue();

        assertNotNull(emailedToken);
        assertNotEquals(emailedToken, mockUser.getEmailVerificationToken());
        assertEquals(TokenHasher.hash(emailedToken), mockUser.getEmailVerificationToken());

        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("Should set the token expiration in the future")
    void shouldSetTokenExpirationInTheFuture() {
        emailVerificationService.issueAndSend(mockUser);

        assertNotNull(mockUser.getEmailVerificationTokenExpiresAt());
        assertTrue(mockUser.getEmailVerificationTokenExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should generate a different token on every issue")
    void shouldGenerateDifferentTokenOnEveryIssue() {
        emailVerificationService.issueAndSend(mockUser);
        String firstToken = mockUser.getEmailVerificationToken();

        emailVerificationService.issueAndSend(mockUser);
        String secondToken = mockUser.getEmailVerificationToken();

        assertNotEquals(firstToken, secondToken);
    }

    @Test
    @DisplayName("Should propagate the failure when sending is explicit")
    void shouldPropagateFailureWhenSendingIsExplicit() {
        doThrow(new IllegalStateException("smtp is down"))
                .when(emailService).sendEmailVerificationEmail(anyString(), anyString(), anyString());

        assertThrows(IllegalStateException.class, () -> emailVerificationService.issueAndSend(mockUser));
    }

    @Test
    @DisplayName("Should swallow the failure when sending quietly")
    void shouldSwallowFailureWhenSendingQuietly() {
        doThrow(new IllegalStateException("smtp is down"))
                .when(emailService).sendEmailVerificationEmail(anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> emailVerificationService.issueAndSendQuietly(mockUser));

        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("Should not confirm the email when the token is issued")
    void shouldNotConfirmEmailWhenTokenIsIssued() {
        emailVerificationService.issueAndSend(mockUser);

        assertNull(mockUser.getEmailConfirmedAt());
        assertFalse(mockUser.isEmailConfirmed());
    }
}

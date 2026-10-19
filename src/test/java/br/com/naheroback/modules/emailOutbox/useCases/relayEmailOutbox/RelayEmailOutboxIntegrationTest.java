package br.com.naheroback.modules.emailOutbox.useCases.relayEmailOutbox;

import br.com.naheroback.common.services.EmailService;
import br.com.naheroback.common.utils.TokenHasher;
import br.com.naheroback.modules.auth.useCases.forgotPassword.ForgotPasswordRequest;
import br.com.naheroback.modules.auth.useCases.forgotPassword.ForgotPasswordUseCase;
import br.com.naheroback.modules.emailOutbox.entities.enums.EmailOutboxStatus;
import br.com.naheroback.modules.emailOutbox.entities.enums.EmailOutboxType;
import br.com.naheroback.modules.user.entities.Role;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.entities.enums.RolesEnum;
import br.com.naheroback.modules.user.repositories.RoleRepository;
import br.com.naheroback.modules.user.repositories.UserRepository;
import br.com.naheroback.modules.user.useCases.user.create.CreateUserRequest;
import br.com.naheroback.modules.user.useCases.user.create.CreateUserUseCase;
import br.com.naheroback.modules.user.useCases.user.resendEmailVerification.ResendEmailVerificationRequest;
import br.com.naheroback.modules.user.useCases.user.resendEmailVerification.ResendEmailVerificationUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = {
        "email-outbox.enabled=false",
        "email-outbox.max-attempts=3",
        "email-outbox.base-backoff-seconds=60",
        "reengagement.enabled=false",
        "payment.reconcile.enabled=false"
})
class RelayEmailOutboxIntegrationTest {

    @Autowired
    private RelayEmailOutboxUseCase relayEmailOutboxUseCase;

    @Autowired
    private ForgotPasswordUseCase forgotPasswordUseCase;

    @Autowired
    private CreateUserUseCase createUserUseCase;

    @Autowired
    private ResendEmailVerificationUseCase resendEmailVerificationUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailService emailService;

    private String email;
    private String signupEmail;
    private Integer userId;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("pt-BR"));

        Role student = roleRepository.findByName(RolesEnum.IS_STUDENT.name()).orElseThrow();

        email = "outbox-%s@example.com".formatted(System.nanoTime());
        signupEmail = "outbox-signup-%s@example.com".formatted(System.nanoTime());

        User user = new User();
        user.setName("Outbox Student");
        user.setEmail(email);
        user.setPassword("irrelevant");
        user.setRoles(new HashSet<>(Set.of(student)));

        userId = userRepository.save(user).getId();
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();

        for (String address : List.of(email, signupEmail)) {
            jdbcTemplate.update("DELETE FROM email_outbox WHERE user_id IN (SELECT id FROM users WHERE email = ?)", address);
            jdbcTemplate.update("DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE email = ?)", address);
            jdbcTemplate.update("DELETE FROM users WHERE email = ?", address);
        }
    }

    @Test
    @DisplayName("Should queue the password reset in the request and deliver it on the next relay")
    void shouldQueueThePasswordResetAndDeliverItOnTheNextRelay() {
        forgotPasswordUseCase.execute(new ForgotPasswordRequest(email));

        verifyNoInteractions(emailService);
        Map<String, Object> queued = outboxRow(userId, EmailOutboxType.PASSWORD_RESET);
        assertEquals(EmailOutboxStatus.PENDING.name(), queued.get("status"));
        assertEquals("pt-BR", queued.get("locale"));
        assertEquals(0, queued.get("attempts"));
        assertNull(storedToken("forgot_password_token", userId));

        relayEmailOutboxUseCase.execute();

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Locale> locale = ArgumentCaptor.forClass(Locale.class);
        verify(emailService).sendPasswordResetEmail(eq(email), anyString(), token.capture(), locale.capture());

        assertEquals("pt", locale.getValue().getLanguage());
        assertEquals(TokenHasher.hash(token.getValue()), storedToken("forgot_password_token", userId));

        Map<String, Object> delivered = outboxRow(userId, EmailOutboxType.PASSWORD_RESET);
        assertEquals(EmailOutboxStatus.SENT.name(), delivered.get("status"));
        assertEquals(1, delivered.get("attempts"));
        assertNotNull(delivered.get("sent_at"));
    }

    @Test
    @DisplayName("Should keep the email pending with a backoff when the mail server fails, then deliver it")
    void shouldRetryAfterTheMailServerFails() {
        doThrow(new IllegalStateException("mail server unavailable"))
                .when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString(), any(Locale.class));

        forgotPasswordUseCase.execute(new ForgotPasswordRequest(email));
        LocalDateTime firstAttemptAt = LocalDateTime.now();
        relayEmailOutboxUseCase.execute();

        Map<String, Object> failedOnce = outboxRow(userId, EmailOutboxType.PASSWORD_RESET);
        assertEquals(EmailOutboxStatus.PENDING.name(), failedOnce.get("status"));
        assertEquals(1, failedOnce.get("attempts"));
        assertTrue(((String) failedOnce.get("last_error")).contains("mail server unavailable"));
        assertTrue(timestamp(failedOnce, "next_attempt_at").isAfter(firstAttemptAt.plusSeconds(55)));
        assertNull(storedToken("forgot_password_token", userId));

        relayEmailOutboxUseCase.execute();

        verify(emailService, times(1)).sendPasswordResetEmail(eq(email), anyString(), anyString(), any(Locale.class));

        reset(emailService);
        makeDue(userId);

        relayEmailOutboxUseCase.execute();

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq(email), anyString(), token.capture(), any(Locale.class));

        Map<String, Object> delivered = outboxRow(userId, EmailOutboxType.PASSWORD_RESET);
        assertEquals(EmailOutboxStatus.SENT.name(), delivered.get("status"));
        assertEquals(2, delivered.get("attempts"));
        assertNull(delivered.get("last_error"));
        assertEquals(TokenHasher.hash(token.getValue()), storedToken("forgot_password_token", userId));
    }

    @Test
    @DisplayName("Should give up after the configured number of attempts")
    void shouldGiveUpAfterTheMaxAttempts() {
        doThrow(new IllegalStateException("mail server unavailable"))
                .when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString(), any(Locale.class));

        forgotPasswordUseCase.execute(new ForgotPasswordRequest(email));

        for (int attempt = 0; attempt < 3; attempt++) {
            relayEmailOutboxUseCase.execute();
            makeDue(userId);
        }

        Map<String, Object> exhausted = outboxRow(userId, EmailOutboxType.PASSWORD_RESET);
        assertEquals(EmailOutboxStatus.FAILED.name(), exhausted.get("status"));
        assertEquals(3, exhausted.get("attempts"));

        relayEmailOutboxUseCase.execute();

        verify(emailService, times(3)).sendPasswordResetEmail(eq(email), anyString(), anyString(), any(Locale.class));
    }

    @Test
    @DisplayName("Should create the user without touching the mail server and deliver the verification later")
    void shouldQueueTheVerificationEmailWithTheSignup() {
        createUserUseCase.execute(new CreateUserRequest("New Student", signupEmail, "password123", null));

        verifyNoInteractions(emailService);
        Integer signupUserId = userRepository.findByEmail(signupEmail).orElseThrow().getId();
        assertEquals(EmailOutboxStatus.PENDING.name(), outboxRow(signupUserId, EmailOutboxType.EMAIL_VERIFICATION).get("status"));

        relayEmailOutboxUseCase.execute();

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendEmailVerificationEmail(eq(signupEmail), anyString(), token.capture(), any(Locale.class));
        assertEquals(TokenHasher.hash(token.getValue()), storedToken("email_verification_token", signupUserId));
        assertEquals(EmailOutboxStatus.SENT.name(), outboxRow(signupUserId, EmailOutboxType.EMAIL_VERIFICATION).get("status"));
    }

    @Test
    @DisplayName("Should skip a queued verification when the email was confirmed before the relay")
    void shouldSkipTheVerificationWhenAlreadyConfirmed() {
        resendEmailVerificationUseCase.execute(new ResendEmailVerificationRequest(email));
        jdbcTemplate.update("UPDATE users SET email_confirmed_at = ? WHERE id = ?", LocalDateTime.now(), userId);

        relayEmailOutboxUseCase.execute();

        verify(emailService, never()).sendEmailVerificationEmail(anyString(), anyString(), anyString(), any(Locale.class));
        assertEquals(EmailOutboxStatus.SKIPPED.name(), outboxRow(userId, EmailOutboxType.EMAIL_VERIFICATION).get("status"));
    }

    private Map<String, Object> outboxRow(Integer ownerId, EmailOutboxType type) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM email_outbox WHERE user_id = ? AND email_type = ?", ownerId, type.name());
    }

    private String storedToken(String column, Integer ownerId) {
        return jdbcTemplate.queryForObject("SELECT " + column + " FROM users WHERE id = ?", String.class, ownerId);
    }

    private void makeDue(Integer ownerId) {
        jdbcTemplate.update("UPDATE email_outbox SET next_attempt_at = ? WHERE user_id = ?",
                LocalDateTime.now().minusSeconds(1), ownerId);
    }

    private LocalDateTime timestamp(Map<String, Object> row, String column) {
        return ((Timestamp) row.get(column)).toLocalDateTime();
    }
}

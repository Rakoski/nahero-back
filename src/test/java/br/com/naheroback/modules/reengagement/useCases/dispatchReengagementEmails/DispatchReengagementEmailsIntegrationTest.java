package br.com.naheroback.modules.reengagement.useCases.dispatchReengagementEmails;

import br.com.naheroback.common.services.EmailService;
import br.com.naheroback.modules.reengagement.entities.enums.ReengagementEmailStatus;
import br.com.naheroback.modules.reengagement.entities.enums.ReengagementEmailType;
import br.com.naheroback.modules.user.entities.Role;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.entities.enums.RolesEnum;
import br.com.naheroback.modules.user.repositories.RoleRepository;
import br.com.naheroback.modules.user.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
        "reengagement.enabled=false",
        "payment.reconcile.enabled=false"
})
class DispatchReengagementEmailsIntegrationTest {

    @Autowired
    private DispatchReengagementEmailsUseCase dispatchReengagementEmailsUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailService emailService;

    private Integer userId;
    private String email;
    private LocalDateTime testStartedAt;

    @BeforeEach
    void setUp() {
        testStartedAt = LocalDateTime.now();

        Role student = roleRepository.findByName(RolesEnum.IS_STUDENT.name()).orElseThrow();

        email = "inactive-%s@example.com".formatted(System.nanoTime());

        User user = new User();
        user.setName("Inactive Student");
        user.setEmail(email);
        user.setPassword("irrelevant");
        user.setEmailConfirmedAt(LocalDateTime.now().minusDays(30));
        user.setRoles(new HashSet<>(Set.of(student)));

        userId = userRepository.save(user).getId();

        jdbcTemplate.update("UPDATE users SET created_at = ? WHERE id = ?",
                LocalDateTime.now().minusDays(15), userId);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM reengagement_dispatch_runs WHERE started_at >= ?", testStartedAt);
        jdbcTemplate.update("DELETE FROM reengagement_emails WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }

    @Test
    @DisplayName("Should keep dispatching on the runs that follow a recorded send")
    void shouldSurviveASecondRunWithExistingHistory() {
        dispatchReengagementEmailsUseCase.execute();

        assertEquals(1, countWithStatus(ReengagementEmailStatus.SENT));

        DispatchReengagementEmailsResponse second = assertDoesNotThrow(
                () -> dispatchReengagementEmailsUseCase.execute());

        assertEquals(0, second.failed());
        assertEquals(1, countWithStatus(ReengagementEmailStatus.SENT));

        verify(emailService, times(1)).sendReengagementEmail(
                eq(email), anyString(), eq(ReengagementEmailType.WE_MISS_YOU.messagePrefix()),
                anyString(), any(Locale.class));
    }

    @Test
    @DisplayName("Should keep a failed attempt on record and retry the candidate")
    void shouldRecordTheFailureAndRetry() {
        doThrow(new IllegalStateException("mail server unavailable"))
                .when(emailService).sendReengagementEmail(anyString(), anyString(), anyString(), anyString(), any(Locale.class));

        dispatchReengagementEmailsUseCase.execute();

        assertEquals(0, countWithStatus(ReengagementEmailStatus.SENT));
        assertEquals(1, countWithStatus(ReengagementEmailStatus.FAILED));
        assertTrue(failureReasons().get(0).contains("mail server unavailable"));

        reset(emailService);

        dispatchReengagementEmailsUseCase.execute();

        assertEquals(1, countWithStatus(ReengagementEmailStatus.SENT));
        verify(emailService).sendReengagementEmail(eq(email), anyString(), anyString(), anyString(), any(Locale.class));
    }

    @Test
    @DisplayName("Should ignore a candidate who has been idle for less than the first step delay")
    void shouldIgnoreCandidatesInsideTheFirstStepDelay() {
        jdbcTemplate.update("UPDATE users SET created_at = ? WHERE id = ?",
                LocalDateTime.now().minusDays(3), userId);

        dispatchReengagementEmailsUseCase.execute();

        assertEquals(0, countWithStatus(ReengagementEmailStatus.SENT));
        verify(emailService, never()).sendReengagementEmail(eq(email), anyString(), anyString(),
                anyString(), any(Locale.class));
    }

    @Test
    @DisplayName("Should hold the candidate inside the cooldown after a delivered step")
    void shouldHoldTheCandidateInsideTheCooldown() {
        dispatchReengagementEmailsUseCase.execute();

        DispatchReengagementEmailsResponse second = dispatchReengagementEmailsUseCase.execute();

        assertEquals(0, second.sent());
        assertEquals(1, countWithStatus(ReengagementEmailStatus.SENT));
        assertEquals(1, countOfType(ReengagementEmailType.WE_MISS_YOU));
        assertEquals(0, countOfType(ReengagementEmailType.STREAK_BROKEN));
    }

    @Test
    @DisplayName("Should link to the default language when the candidate has no practice attempt")
    void shouldLinkToTheDefaultLanguage() {
        dispatchReengagementEmailsUseCase.execute();

        verify(emailService).sendReengagementEmail(eq(email), anyString(), anyString(),
                endsWith("/pt/practice-exams"), eq(Locale.forLanguageTag("pt")));
    }

    @Test
    @DisplayName("Should persist the run with a queryable eligibility funnel")
    void shouldPersistTheRunWithItsFunnel() {
        dispatchReengagementEmailsUseCase.execute();

        Map<String, Object> run = jdbcTemplate.queryForMap("""
            SELECT candidates, sent, failed, skipped,
                   (funnel ->> 'eligible')::int AS eligible,
                   (funnel ->> 'students')::int AS students
            FROM reengagement_dispatch_runs
            ORDER BY id DESC LIMIT 1
        """);

        assertTrue((Integer) run.get("candidates") >= 1);
        assertTrue((Integer) run.get("sent") >= 1);
        assertEquals(0, run.get("failed"));
        assertNotNull(run.get("eligible"));
        assertTrue((Integer) run.get("students") >= 1);
    }

    private int countWithStatus(ReengagementEmailStatus status) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT count(*) FROM reengagement_emails
            WHERE user_id = ? AND status = ? AND deleted_at IS NULL
        """, Integer.class, userId, status.name());

        return count == null ? 0 : count;
    }

    private int countOfType(ReengagementEmailType type) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT count(*) FROM reengagement_emails
            WHERE user_id = ? AND email_type = ? AND deleted_at IS NULL
        """, Integer.class, userId, type.name());

        return count == null ? 0 : count;
    }

    private List<String> failureReasons() {
        return jdbcTemplate.queryForList("""
            SELECT failure_reason FROM reengagement_emails
            WHERE user_id = ? AND status = 'FAILED' AND deleted_at IS NULL
        """, String.class, userId);
    }
}

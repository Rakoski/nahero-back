package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt;

import br.com.naheroback.common.exceptions.custom.PaymentRequiredException;
import br.com.naheroback.modules.auth.entities.AuthenticatedUser;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getDashboardSummary.GetDashboardSummaryResponse;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getDashboardSummary.GetDashboardSummaryUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getHistory.GetHistoryFilterDTO;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getHistory.GetHistoryResponse;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getHistory.GetHistoryUseCase;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Practice exams themselves are free; the study feedback built on top of them — the dashboard
 * and the attempt history — is what Premium buys.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "reengagement.enabled=false",
        "payment.reconcile.enabled=false"
})
class StudyFeedbackEntitlementIntegrationTest {

    @Autowired
    private GetDashboardSummaryUseCase getDashboardSummary;

    @Autowired
    private GetHistoryUseCase getHistory;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Integer studentId;

    @BeforeEach
    void setUp() {
        Role student = roleRepository.findByName(RolesEnum.IS_STUDENT.name()).orElseThrow();

        User user = new User();
        user.setName("Dashboard Student");
        user.setEmail("dashboard-%s@example.com".formatted(System.nanoTime()));
        user.setPassword("irrelevant");
        user.setRoles(new HashSet<>(Set.of(student)));

        User saved = userRepository.save(user);
        studentId = saved.getId();

        AuthenticatedUser principal = new AuthenticatedUser(saved);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("DELETE FROM subscriptions WHERE user_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", studentId);
    }

    @Test
    @DisplayName("Should refuse the study feedback dashboard without an active subscription")
    void shouldRefuseTheDashboardWithoutASubscription() {
        assertThrows(PaymentRequiredException.class, () -> getDashboardSummary.execute());
    }

    @Test
    @DisplayName("Should refuse the study feedback dashboard once the subscription has lapsed")
    void shouldRefuseTheDashboardWithAnExpiredSubscription() {
        insertSubscription(OffsetDateTime.now().minusDays(1));

        assertThrows(PaymentRequiredException.class, () -> getDashboardSummary.execute());
    }

    @Test
    @DisplayName("Should serve the study feedback dashboard to an active subscriber")
    void shouldServeTheDashboardToASubscriber() {
        insertSubscription(OffsetDateTime.now().plusDays(30));

        GetDashboardSummaryResponse summary = assertDoesNotThrow(() -> getDashboardSummary.execute());

        assertNotNull(summary);
        assertEquals(0, summary.getTotalAttempts());
    }

    @Test
    @DisplayName("Should refuse the attempt history without an active subscription")
    void shouldRefuseTheHistoryWithoutASubscription() {
        assertThrows(PaymentRequiredException.class,
                () -> getHistory.execute(emptyFilter(), PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("Should refuse the attempt history once the subscription has lapsed")
    void shouldRefuseTheHistoryWithAnExpiredSubscription() {
        insertSubscription(OffsetDateTime.now().minusDays(1));

        assertThrows(PaymentRequiredException.class,
                () -> getHistory.execute(emptyFilter(), PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("Should serve the attempt history to an active subscriber")
    void shouldServeTheHistoryToASubscriber() {
        insertSubscription(OffsetDateTime.now().plusDays(30));

        Page<GetHistoryResponse> history = assertDoesNotThrow(
                () -> getHistory.execute(emptyFilter(), PageRequest.of(0, 10)));

        assertNotNull(history);
        assertEquals(0, history.getTotalElements());
    }

    private GetHistoryFilterDTO emptyFilter() {
        return new GetHistoryFilterDTO(null, null, null, null);
    }

    private void insertSubscription(OffsetDateTime currentPeriodEnd) {
        jdbcTemplate.update("""
                INSERT INTO subscriptions (user_id, provider, external_subscription_id, status,
                                           current_period_end, cancel_at_period_end)
                VALUES (?, 'STRIPE', ?, 'active', ?, false)
                """, studentId, "sub_test_%s".formatted(System.nanoTime()), currentPeriodEnd);
    }
}

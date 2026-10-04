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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
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

    @Test
    @DisplayName("Should count answered and correct questions per period on the dashboard")
    void shouldCountQuestionsPerPeriod() {
        insertSubscription(OffsetDateTime.now().plusDays(30));
        Integer examId = jdbcTemplate.queryForObject(
                "INSERT INTO exams (title, difficulty_level, is_active) VALUES ('Dashboard Exam', 2, true) RETURNING id",
                Integer.class);
        Integer practiceExamId = jdbcTemplate.queryForObject("""
                INSERT INTO practice_exams (exam_id, title, slug, passing_score, time_limit, number_of_questions, is_active)
                VALUES (?, 'Dashboard Practice', ?, 50, 60, 3, true) RETURNING id
                """, Integer.class, examId, "dashboard-practice-" + System.nanoTime());
        Integer enrollmentId = jdbcTemplate.queryForObject(
                "INSERT INTO enrollments (student_id, exam_id) VALUES (?, ?) RETURNING id", Integer.class, studentId, examId);

        try {
            Integer first = insertQuestion(practiceExamId);
            Integer second = insertQuestion(practiceExamId);
            Integer third = insertQuestion(practiceExamId);

            LocalDateTime oldEnd = LocalDateTime.now().minusDays(40);
            Integer recent = insertAttempt(enrollmentId, practiceExamId, LocalDateTime.now().minusDays(3));
            Integer old = insertAttempt(enrollmentId, practiceExamId, oldEnd);

            insertAnswer(recent, first, alternativeOf(first, true), true);
            insertAnswer(recent, first, alternativeOf(first, false), true);
            insertAnswer(recent, second, alternativeOf(second, false), false);
            insertAnswer(recent, third, null, false);
            insertAnswer(old, first, alternativeOf(first, true), true);
            insertAnswer(old, second, alternativeOf(second, true), true);

            GetDashboardSummaryResponse.QuestionActivity activity = getDashboardSummary.execute().getQuestionActivity();

            assertWindow(activity, 7, 2, 1);
            assertWindow(activity, 30, 2, 1);
            assertWindow(activity, 90, 4, 3);
            assertWindow(activity, null, 4, 3);
            assertEquals(oldEnd.plusMinutes(30).withNano(0), activity.getSince().withNano(0));
        } finally {
            jdbcTemplate.update("""
                    DELETE FROM student_answers WHERE student_practice_attempt_id IN
                        (SELECT id FROM student_practice_attempts WHERE practice_exam_id = ?)
                    """, practiceExamId);
            jdbcTemplate.update("DELETE FROM student_practice_attempts WHERE practice_exam_id = ?", practiceExamId);
            jdbcTemplate.update("DELETE FROM enrollments WHERE id = ?", enrollmentId);
            jdbcTemplate.update("DELETE FROM alternatives WHERE question_id IN (SELECT id FROM questions WHERE practice_exam_id = ?)", practiceExamId);
            jdbcTemplate.update("DELETE FROM questions WHERE practice_exam_id = ?", practiceExamId);
            jdbcTemplate.update("DELETE FROM practice_exams WHERE id = ?", practiceExamId);
            jdbcTemplate.update("DELETE FROM exams WHERE id = ?", examId);
        }
    }

    private void assertWindow(GetDashboardSummaryResponse.QuestionActivity activity, Integer days, long answered, long correct) {
        GetDashboardSummaryResponse.QuestionWindow window = activity.getWindows().stream()
                .filter(candidate -> Objects.equals(candidate.getDays(), days))
                .findFirst()
                .orElseThrow();
        assertEquals(answered, window.getAnswered(), "answered in window " + days);
        assertEquals(correct, window.getCorrect(), "correct in window " + days);
    }

    private Integer insertQuestion(Integer practiceExamId) {
        Integer questionId = jdbcTemplate.queryForObject("""
                INSERT INTO questions (practice_exam_id, question_type_id, content, points, version, is_active, language)
                VALUES (?, 1, 'Question', 1, 1, true, 'en') RETURNING id
                """, Integer.class, practiceExamId);
        jdbcTemplate.update("""
                INSERT INTO alternatives (question_id, content, is_correct, version, is_active)
                VALUES (?, 'Right', true, 1, true), (?, 'Wrong', false, 1, true)
                """, questionId, questionId);
        return questionId;
    }

    private Integer alternativeOf(Integer questionId, boolean correct) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM alternatives WHERE question_id = ? AND is_correct = ?", Integer.class, questionId, correct);
    }

    private Integer insertAttempt(Integer enrollmentId, Integer practiceExamId, LocalDateTime startedAt) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO student_practice_attempts (enrollment_id, practice_exam_id, status, start_time, end_time,
                                                       score, passed, language)
                VALUES (?, ?, 2, ?, ?, 1, false, 'en') RETURNING id
                """, Integer.class, enrollmentId, practiceExamId, startedAt, startedAt.plusMinutes(30));
    }

    private void insertAnswer(Integer attemptId, Integer questionId, Integer alternativeId, boolean correct) {
        jdbcTemplate.update("""
                INSERT INTO student_answers (student_practice_attempt_id, question_id, question_version,
                                             selected_alternative_id, selected_alternative_version, is_correct)
                VALUES (?, ?, 1, ?, ?, ?)
                """, attemptId, questionId, alternativeId, alternativeId == null ? null : 1, correct);
    }

    private GetHistoryFilterDTO emptyFilter() {
        return new GetHistoryFilterDTO(null, null, null, null);
    }

    private void insertSubscription(OffsetDateTime currentPeriodEnd) {
        jdbcTemplate.update("""
                INSERT INTO subscriptions (user_id, provider, external_subscription_id, status,
                                           current_period_end, cancel_at_period_end)
                VALUES (?, 'STRIPE', ?, 'ACTIVE', ?, false)
                """, studentId, "sub_test_%s".formatted(System.nanoTime()), currentPeriodEnd);
    }
}

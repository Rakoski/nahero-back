package br.com.naheroback.modules.practiceExams.useCases.answer;

import br.com.naheroback.modules.auth.entities.AuthenticatedUser;
import br.com.naheroback.modules.practiceExams.useCases.answer.listAnswered.AnswerFilterDTO;
import br.com.naheroback.modules.practiceExams.useCases.answer.listAnswered.ListAnsweredAnswersResponse;
import br.com.naheroback.modules.practiceExams.useCases.answer.listAnswered.ListAnsweredAnswersUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.create.CreateStudentPracticeAttemptRequest;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.create.CreateStudentPracticeAttemptUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.finish.FinishStudentPracticeAttemptRequest;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.finish.FinishStudentPracticeAttemptUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getResult.GetResultResponse;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getResult.GetResultUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.saveProgress.SaveAttemptProgressRequest;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.saveProgress.SaveAttemptProgressUseCase;
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
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The practice exam and its result are free; the per-question explanation is the paid part,
 * so it must never leave the API for a student without an active subscription.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "reengagement.enabled=false",
        "payment.reconcile.enabled=false"
})
class ExplanationEntitlementIntegrationTest {

    private static final int QUESTIONS_PER_EXAM = 2;
    private static final String EXPLANATION = "S3 is object storage, not a block device.";

    @Autowired
    private CreateStudentPracticeAttemptUseCase createAttempt;

    @Autowired
    private SaveAttemptProgressUseCase saveProgress;

    @Autowired
    private FinishStudentPracticeAttemptUseCase finishAttempt;

    @Autowired
    private ListAnsweredAnswersUseCase listAnswers;

    @Autowired
    private GetResultUseCase getResult;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Integer studentId;
    private Integer examId;
    private Integer practiceExamId;
    private Integer attemptId;

    @BeforeEach
    void setUp() {
        Role student = roleRepository.findByName(RolesEnum.IS_STUDENT.name()).orElseThrow();

        User user = new User();
        user.setName("Explanation Student");
        user.setEmail("explanation-%s@example.com".formatted(System.nanoTime()));
        user.setPassword("irrelevant");
        user.setRoles(new HashSet<>(Set.of(student)));

        User saved = userRepository.save(user);
        studentId = saved.getId();

        AuthenticatedUser principal = new AuthenticatedUser(saved);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        examId = jdbcTemplate.queryForObject("""
                INSERT INTO exams (title, difficulty_level, is_active)
                VALUES ('Explanation Exam', 2, true) RETURNING id
                """, Integer.class);

        practiceExamId = jdbcTemplate.queryForObject("""
                INSERT INTO practice_exams (exam_id, title, slug, passing_score, time_limit, number_of_questions, is_active)
                VALUES (?, 'Explanation Practice', ?, 50, 60, ?, true) RETURNING id
                """, Integer.class, examId, "explanation-practice-" + System.nanoTime(), QUESTIONS_PER_EXAM);

        List<Integer> questionIds = java.util.stream.IntStream.range(0, QUESTIONS_PER_EXAM)
                .mapToObj(index -> {
                    Integer questionId = jdbcTemplate.queryForObject("""
                            INSERT INTO questions (practice_exam_id, question_type_id, content, explanation,
                                                   points, version, is_active, language)
                            VALUES (?, 3, ?, ?, 1, 1, true, 'en') RETURNING id
                            """, Integer.class, practiceExamId, "Question %d".formatted(index), EXPLANATION);

                    jdbcTemplate.update("""
                            INSERT INTO alternatives (question_id, content, is_correct, version, is_active)
                            VALUES (?, 'Correct', true, 1, true), (?, 'Wrong', false, 1, true)
                            """, questionId, questionId);

                    return questionId;
                })
                .toList();

        attemptId = createAttempt.execute(
                new CreateStudentPracticeAttemptRequest(practiceExamId, null), Locale.ENGLISH);

        for (Integer questionId : questionIds) {
            saveProgress.execute(attemptId,
                    new SaveAttemptProgressRequest(questionId, List.of(correctAlternativeOf(questionId)), 0));
        }

        finishAttempt.execute(new FinishStudentPracticeAttemptRequest(attemptId, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();

        jdbcTemplate.update("""
                DELETE FROM student_attempt_answer_drafts WHERE student_practice_attempt_id IN
                    (SELECT id FROM student_practice_attempts WHERE practice_exam_id = ?)
                """, practiceExamId);
        jdbcTemplate.update("""
                DELETE FROM student_answers WHERE student_practice_attempt_id IN
                    (SELECT id FROM student_practice_attempts WHERE practice_exam_id = ?)
                """, practiceExamId);
        jdbcTemplate.update("DELETE FROM student_practice_attempts WHERE practice_exam_id = ?", practiceExamId);
        jdbcTemplate.update("DELETE FROM enrollments WHERE student_id = ?", studentId);
        jdbcTemplate.update(
                "DELETE FROM alternatives WHERE question_id IN (SELECT id FROM questions WHERE practice_exam_id = ?)",
                practiceExamId);
        jdbcTemplate.update("DELETE FROM questions WHERE practice_exam_id = ?", practiceExamId);
        jdbcTemplate.update("DELETE FROM practice_exams WHERE id = ?", practiceExamId);
        jdbcTemplate.update("DELETE FROM exams WHERE id = ?", examId);
        jdbcTemplate.update("DELETE FROM subscriptions WHERE user_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", studentId);
    }

    @Test
    @DisplayName("Should withhold the explanation from a student without an active subscription")
    void shouldWithholdTheExplanationFromAFreeStudent() {
        List<ListAnsweredAnswersResponse> answers = listAnsweredAnswers();

        assertEquals(QUESTIONS_PER_EXAM, answers.size());
        answers.forEach(answer -> {
            assertNull(answer.getExplanation());
            assertTrue(answer.getExplanationLocked());
        });
    }

    @Test
    @DisplayName("Should hand the explanation to an active subscriber")
    void shouldHandTheExplanationToASubscriber() {
        insertSubscription(OffsetDateTime.now().plusDays(30));

        List<ListAnsweredAnswersResponse> answers = listAnsweredAnswers();

        assertEquals(QUESTIONS_PER_EXAM, answers.size());
        answers.forEach(answer -> {
            assertEquals(EXPLANATION, answer.getExplanation());
            assertFalse(answer.getExplanationLocked());
        });
    }

    @Test
    @DisplayName("Should still serve the result page and the answer review to a free student")
    void shouldKeepTheResultPageFree() {
        GetResultResponse result = assertDoesNotThrow(() -> getResult.execute(attemptId));

        assertEquals(QUESTIONS_PER_EXAM, result.getCorrectAnswers());
        assertEquals(0, result.getIncorrectAnswers());

        List<ListAnsweredAnswersResponse> answers = assertDoesNotThrow(this::listAnsweredAnswers);

        assertEquals(QUESTIONS_PER_EXAM, answers.size());
        assertTrue(answers.stream().noneMatch(answer -> answer.getAlternatives().isEmpty()));
    }

    private List<ListAnsweredAnswersResponse> listAnsweredAnswers() {
        Page<ListAnsweredAnswersResponse> page = listAnswers.execute(
                attemptId, new AnswerFilterDTO(null, null), PageRequest.of(0, 10));
        return page.getContent();
    }

    private Integer correctAlternativeOf(Integer questionId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM alternatives WHERE question_id = ? AND is_correct = true", Integer.class, questionId);
    }

    private void insertSubscription(OffsetDateTime currentPeriodEnd) {
        jdbcTemplate.update("""
                INSERT INTO subscriptions (user_id, provider, external_subscription_id, status,
                                           current_period_end, cancel_at_period_end)
                VALUES (?, 'STRIPE', ?, 'ACTIVE', ?, false)
                """, studentId, "sub_test_%s".formatted(System.nanoTime()), currentPeriodEnd);
    }
}

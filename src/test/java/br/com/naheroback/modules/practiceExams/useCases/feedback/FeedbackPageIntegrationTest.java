package br.com.naheroback.modules.practiceExams.useCases.feedback;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.common.exceptions.custom.PaymentRequiredException;
import br.com.naheroback.modules.auth.entities.AuthenticatedUser;
import br.com.naheroback.modules.practiceExams.controllers.FeedbackController;
import br.com.naheroback.modules.practiceExams.controllers.StudentPracticeAttemptController;
import br.com.naheroback.modules.practiceExams.services.AttemptDomainBreakdownService.DomainScore;
import br.com.naheroback.modules.practiceExams.services.StudyPlan;
import br.com.naheroback.modules.practiceExams.useCases.feedback.answerPracticeQuestion.AnswerPracticeQuestionRequest;
import br.com.naheroback.modules.practiceExams.useCases.feedback.answerPracticeQuestion.AnswerPracticeQuestionResponse;
import br.com.naheroback.modules.practiceExams.useCases.feedback.getFeedbackPage.GetFeedbackPageResponse;
import br.com.naheroback.modules.practiceExams.useCases.feedback.getFeedbackPage.GetFeedbackPageResponse.PracticeQuestion;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getFeedback.GetAttemptFeedbackResponse;
import br.com.naheroback.modules.user.entities.Role;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.entities.enums.RolesEnum;
import br.com.naheroback.modules.user.repositories.RoleRepository;
import br.com.naheroback.modules.user.repositories.UserRepository;
import br.com.naheroback.providers.deepseek.DeepSeekClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
        "reengagement.enabled=false",
        "payment.reconcile.enabled=false"
})
class FeedbackPageIntegrationTest {

    private static final String SECURITY = "Security and Compliance";
    private static final String CONCEPTS = "Cloud Concepts";

    @Autowired
    private FeedbackController feedbackController;

    @Autowired
    private StudentPracticeAttemptController attemptController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private DeepSeekClient deepSeekClient;

    private final List<Integer> userIds = new ArrayList<>();
    private final List<Integer> examIds = new ArrayList<>();
    private final List<Integer> practiceExamIds = new ArrayList<>();

    private Integer studentId;
    private Integer practiceExamId;
    private String practiceExamSlug;
    private Integer olderAttemptId;
    private Integer latestAttemptId;
    private Integer seenCorrectQuestionId;
    private Integer missedQuestionId;
    private List<Integer> unseenQuestionIds;
    private Integer otherLanguageQuestionId;
    private Integer descriptiveQuestionId;

    @BeforeEach
    void setUp() {
        studentId = createStudent("feedback-page");
        practiceExamSlug = "feedback-page-" + System.nanoTime();
        practiceExamId = createPracticeExam("Feedback Page Practice", practiceExamSlug);
        Integer enrollmentId = enroll(studentId, practiceExamId);

        seenCorrectQuestionId = insertQuestion(practiceExamId, SECURITY, "Seen and right", "en", 3);
        missedQuestionId = insertQuestion(practiceExamId, SECURITY, "Seen and wrong", "en", 3);
        unseenQuestionIds = List.of(
                insertQuestion(practiceExamId, SECURITY, "Unseen 1", "en", 3),
                insertQuestion(practiceExamId, SECURITY, "Unseen 2", "en", 3),
                insertQuestion(practiceExamId, SECURITY, "Unseen 3", "en", 3));
        otherLanguageQuestionId = insertQuestion(practiceExamId, SECURITY, "Não vista", "pt", 3);
        descriptiveQuestionId = insertQuestion(practiceExamId, SECURITY, "Explain IAM", "en", 4);
        Integer conceptsQuestionA = insertQuestion(practiceExamId, CONCEPTS, "Elasticity", "en", 3);
        Integer conceptsQuestionB = insertQuestion(practiceExamId, CONCEPTS, "Agility", "en", 3);

        olderAttemptId = insertAttempt(enrollmentId, practiceExamId, LocalDateTime.now().minusDays(3), 2, 2);
        insertAnswer(olderAttemptId, seenCorrectQuestionId, true);
        insertAnswer(olderAttemptId, conceptsQuestionA, true);

        latestAttemptId = insertAttempt(enrollmentId, practiceExamId, LocalDateTime.now().minusHours(2), 2, 1);
        insertAnswer(latestAttemptId, missedQuestionId, false);
        insertAnswer(latestAttemptId, conceptsQuestionB, true);

        insertAttempt(enrollmentId, practiceExamId, LocalDateTime.now().minusMinutes(5), 1, null);

        authenticateAs(studentId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        for (Integer id : practiceExamIds) {
            jdbcTemplate.update("""
                    DELETE FROM attempt_feedback WHERE attempt_id IN
                        (SELECT id FROM student_practice_attempts WHERE practice_exam_id = ?)
                    """, id);
            jdbcTemplate.update("""
                    DELETE FROM student_answers WHERE student_practice_attempt_id IN
                        (SELECT id FROM student_practice_attempts WHERE practice_exam_id = ?)
                    """, id);
            jdbcTemplate.update("DELETE FROM student_practice_attempts WHERE practice_exam_id = ?", id);
            jdbcTemplate.update("DELETE FROM alternatives WHERE question_id IN (SELECT id FROM questions WHERE practice_exam_id = ?)", id);
            jdbcTemplate.update("DELETE FROM questions WHERE practice_exam_id = ?", id);
        }
        for (Integer id : userIds) {
            jdbcTemplate.update("DELETE FROM enrollments WHERE student_id = ?", id);
            jdbcTemplate.update("DELETE FROM subscriptions WHERE user_id = ?", id);
            jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", id);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
        }
        practiceExamIds.forEach(id -> jdbcTemplate.update("DELETE FROM practice_exams WHERE id = ?", id));
        examIds.forEach(id -> jdbcTemplate.update("DELETE FROM exams WHERE id = ?", id));
    }

    @Test
    @DisplayName("Should give a premium student the last attempts, combined gaps and weakest-domain practice questions")
    void shouldServeTheFullPageToAPremiumStudent() {
        insertSubscription(studentId);

        GetFeedbackPageResponse page = feedbackController.getPage(practiceExamSlug);

        assertEquals(practiceExamSlug, page.practiceExam().slug());
        assertEquals(List.of(latestAttemptId, olderAttemptId),
                page.attempts().stream().map(GetFeedbackPageResponse.AttemptSummary::attemptId).toList());
        assertEquals(List.of(new DomainScore(SECURITY, 1, 2), new DomainScore(CONCEPTS, 2, 2)), page.domains());
        assertEquals(SECURITY, page.weakestDomain());
        assertEquals(latestAttemptId, page.latestAttemptId());

        List<Integer> questionIds = page.practiceQuestions().stream().map(PracticeQuestion::questionId).toList();
        assertEquals(4, questionIds.size());
        assertTrue(questionIds.subList(0, 3).containsAll(unseenQuestionIds));
        assertEquals(missedQuestionId, questionIds.get(3));
        assertFalse(questionIds.contains(seenCorrectQuestionId));
        assertFalse(questionIds.contains(otherLanguageQuestionId));
        assertFalse(questionIds.contains(descriptiveQuestionId));
        assertEquals(2, page.practiceQuestions().getFirst().alternatives().size());
        assertEquals("OBJECTIVE", page.practiceQuestions().getFirst().type());
    }

    @Test
    @DisplayName("Should refuse the feedback page to a free student")
    void shouldRefuseThePageToAFreeStudent() {
        assertThrows(PaymentRequiredException.class, () -> feedbackController.getPage(practiceExamSlug));
    }

    @Test
    @DisplayName("Should refuse the feedback page once the subscription has lapsed")
    void shouldRefuseThePageWithALapsedSubscription() {
        insertSubscription(studentId, OffsetDateTime.now().minusDays(1));

        assertThrows(PaymentRequiredException.class, () -> feedbackController.getPage(practiceExamSlug));
    }

    @Test
    @DisplayName("Should return an empty page to a subscriber who has not finished any attempt")
    void shouldReturnAnEmptyPageWithoutAttempts() {
        Integer newcomer = createStudent("feedback-newcomer");
        insertSubscription(newcomer);
        authenticateAs(newcomer);

        GetFeedbackPageResponse page = feedbackController.getPage(null);

        assertTrue(page.exams().isEmpty());
        assertNull(page.attempts());
    }

    @Test
    @DisplayName("Should refuse a practice exam the student has not taken")
    void shouldRefuseAnExamTheStudentHasNotTaken() {
        insertSubscription(studentId);

        assertThrows(NotFoundException.class, () -> feedbackController.getPage("not-taken-" + System.nanoTime()));
    }

    @Test
    @DisplayName("Should check a practice answer and return the correct alternatives and the explanation")
    void shouldCheckAPracticeAnswer() {
        insertSubscription(studentId);
        Integer questionId = unseenQuestionIds.getFirst();

        AnswerPracticeQuestionResponse right = feedbackController.answer(questionId,
                new AnswerPracticeQuestionRequest(List.of(correctAlternativeOf(questionId))));
        AnswerPracticeQuestionResponse wrong = feedbackController.answer(questionId,
                new AnswerPracticeQuestionRequest(List.of(wrongAlternativeOf(questionId))));

        assertTrue(right.correct());
        assertFalse(wrong.correct());
        assertEquals(List.of(correctAlternativeOf(questionId)), wrong.correctAlternativeIds());
        assertEquals("Because Unseen 1", wrong.explanation());
    }

    @Test
    @DisplayName("Should refuse practice answers to a free student")
    void shouldRefusePracticeAnswersToAFreeStudent() {
        Integer questionId = unseenQuestionIds.getFirst();

        assertThrows(PaymentRequiredException.class, () -> feedbackController.answer(questionId,
                new AnswerPracticeQuestionRequest(List.of(correctAlternativeOf(questionId)))));
    }

    @Test
    @DisplayName("Should not reveal answers for a practice exam the student never took")
    void shouldNotRevealAnswersForAnotherExam() {
        insertSubscription(studentId);
        Integer otherPracticeExamId = createPracticeExam("Other Practice", "other-" + System.nanoTime());
        Integer foreignQuestionId = insertQuestion(otherPracticeExamId, SECURITY, "Foreign", "en", 3);

        assertThrows(NotFoundException.class, () -> feedbackController.answer(foreignQuestionId,
                new AnswerPracticeQuestionRequest(List.of(correctAlternativeOf(foreignQuestionId)))));
    }

    @Test
    @DisplayName("Should let a subscriber request the study plan from the feedback page")
    void shouldOpenTheStudyPlanToASubscriber() {
        insertSubscription(studentId);
        when(deepSeekClient.model()).thenReturn("deepseek-flash");
        when(deepSeekClient.completeJson(anyString(), anyString(), eq(StudyPlan.class)))
                .thenThrow(new IllegalStateException("DeepSeek call failed after 2 attempts"));

        ResponseEntity<GetAttemptFeedbackResponse> plan = attemptController.getFeedback(latestAttemptId, true);

        assertEquals(HttpStatus.ACCEPTED, plan.getStatusCode());
        assertNotNull(plan.getBody());
        assertFalse(plan.getBody().locked());
        waitForFeedbackRow(latestAttemptId);
    }

    private Integer createStudent(String prefix) {
        Role student = roleRepository.findByName(RolesEnum.IS_STUDENT.name()).orElseThrow();
        User user = new User();
        user.setName("Feedback Page Student");
        user.setEmail("%s-%s@example.com".formatted(prefix, System.nanoTime()));
        user.setPassword("irrelevant");
        user.setRoles(new HashSet<>(Set.of(student)));
        Integer id = userRepository.save(user).getId();
        userIds.add(id);
        return id;
    }

    private void authenticateAs(Integer userId) {
        AuthenticatedUser principal = new AuthenticatedUser(userRepository.findById(userId).orElseThrow());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Integer createPracticeExam(String title, String slug) {
        Integer examId = jdbcTemplate.queryForObject("""
                INSERT INTO exams (title, difficulty_level, is_active) VALUES (?, 2, true) RETURNING id
                """, Integer.class, title + " Exam");
        examIds.add(examId);
        Integer id = jdbcTemplate.queryForObject("""
                INSERT INTO practice_exams (exam_id, title, slug, passing_score, time_limit, number_of_questions, is_active)
                VALUES (?, ?, ?, 50, 60, 2, true) RETURNING id
                """, Integer.class, examId, title, slug);
        practiceExamIds.add(id);
        return id;
    }

    private Integer enroll(Integer userId, Integer practiceExamId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO enrollments (student_id, exam_id)
                VALUES (?, (SELECT exam_id FROM practice_exams WHERE id = ?)) RETURNING id
                """, Integer.class, userId, practiceExamId);
    }

    private Integer insertQuestion(Integer practiceExamId, String domain, String content, String language, int typeId) {
        Integer questionId = jdbcTemplate.queryForObject("""
                INSERT INTO questions (practice_exam_id, question_type_id, content, explanation, points, version,
                                       is_active, language, domain)
                VALUES (?, ?, ?, ?, 1, 1, true, ?, ?) RETURNING id
                """, Integer.class, practiceExamId, typeId, content, "Because " + content, language, domain);
        jdbcTemplate.update("""
                INSERT INTO alternatives (question_id, content, is_correct, version, is_active)
                VALUES (?, 'Right', true, 1, true), (?, 'Wrong', false, 1, true)
                """, questionId, questionId);
        return questionId;
    }

    private Integer insertAttempt(Integer enrollmentId, Integer practiceExamId, LocalDateTime startedAt, int status, Integer score) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO student_practice_attempts (enrollment_id, practice_exam_id, status, start_time, end_time,
                                                       score, passed, language)
                VALUES (?, ?, ?, ?, ?, ?, false, 'en') RETURNING id
                """, Integer.class, enrollmentId, practiceExamId, status, startedAt,
                score == null ? null : startedAt.plusMinutes(30), score);
    }

    private void insertAnswer(Integer attemptId, Integer questionId, boolean correct) {
        jdbcTemplate.update("""
                INSERT INTO student_answers (student_practice_attempt_id, question_id, question_version, is_correct)
                VALUES (?, ?, 1, ?)
                """, attemptId, questionId, correct);
    }

    private void insertSubscription(Integer userId) {
        insertSubscription(userId, OffsetDateTime.now().plusDays(30));
    }

    private void insertSubscription(Integer userId, OffsetDateTime currentPeriodEnd) {
        jdbcTemplate.update("""
                INSERT INTO subscriptions (user_id, provider, external_subscription_id, status,
                                           current_period_end, cancel_at_period_end)
                VALUES (?, 'STRIPE', ?, 'ACTIVE', ?, false)
                """, userId, "sub_test_%s".formatted(System.nanoTime()), currentPeriodEnd);
    }

    private Integer correctAlternativeOf(Integer questionId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM alternatives WHERE question_id = ? AND is_correct = true", Integer.class, questionId);
    }

    private Integer wrongAlternativeOf(Integer questionId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM alternatives WHERE question_id = ? AND is_correct = false", Integer.class, questionId);
    }

    private void waitForFeedbackRow(Integer attemptId) {
        for (int i = 0; i < 100; i++) {
            Integer rows = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM attempt_feedback WHERE attempt_id = ?", Integer.class, attemptId);
            if (rows != null && rows > 0) return;
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        fail("the background generation did not record an attempt");
    }
}

package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt;

import br.com.naheroback.modules.auth.entities.AuthenticatedUser;
import br.com.naheroback.modules.practiceExams.controllers.StudentPracticeAttemptController;
import br.com.naheroback.modules.practiceExams.services.AttemptFeedbackService;
import br.com.naheroback.modules.practiceExams.services.StudyPlan;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getFeedback.GetAttemptFeedbackResponse;
import br.com.naheroback.modules.user.entities.Role;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.entities.enums.RolesEnum;
import br.com.naheroback.modules.user.repositories.RoleRepository;
import br.com.naheroback.modules.user.repositories.UserRepository;
import br.com.naheroback.providers.deepseek.DeepSeekClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.AopTestUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = {
        "reengagement.enabled=false",
        "payment.reconcile.enabled=false"
})
class AttemptFeedbackIntegrationTest {

    private static final String VALID_PLAN = """
            {
              "summary": "You are close to the passing line. Security is where most points were lost.",
              "priorities": [
                {"domain": "Security and Compliance", "why": "Missed IAM questions.", "whatToStudy": "IAM policies."},
                {"domain": "Cloud Technology and Services", "why": "Missed storage questions.", "whatToStudy": "S3 classes."},
                {"domain": "Cloud Concepts", "why": "One missed question.", "whatToStudy": "Well-Architected pillars."}
              ],
              "plan": ["IAM users and roles", "IAM policies", "S3 storage classes", "Shared responsibility", "Well-Architected"]
            }
            """;

    @Autowired
    private AttemptFeedbackService attemptFeedbackService;

    @Autowired
    private StudentPracticeAttemptController controller;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeepSeekClient deepSeekClient;

    private Integer studentId;
    private Integer examId;
    private Integer practiceExamId;
    private Integer enrollmentId;
    private Integer attemptId;

    @BeforeEach
    void setUp() {
        Role student = roleRepository.findByName(RolesEnum.IS_STUDENT.name()).orElseThrow();

        User user = new User();
        user.setName("Feedback Student");
        user.setEmail("feedback-%s@example.com".formatted(System.nanoTime()));
        user.setPassword("irrelevant");
        user.setRoles(new HashSet<>(Set.of(student)));

        User saved = userRepository.save(user);
        studentId = saved.getId();

        examId = jdbcTemplate.queryForObject("""
                INSERT INTO exams (title, difficulty_level, is_active) VALUES ('Feedback Exam', 2, true) RETURNING id
                """, Integer.class);
        practiceExamId = jdbcTemplate.queryForObject("""
                INSERT INTO practice_exams (exam_id, title, slug, passing_score, time_limit, number_of_questions, is_active)
                VALUES (?, 'Feedback Practice', ?, 50, 60, 4, true) RETURNING id
                """, Integer.class, examId, "feedback-practice-" + System.nanoTime());
        enrollmentId = jdbcTemplate.queryForObject(
                "INSERT INTO enrollments (student_id, exam_id) VALUES (?, ?) RETURNING id",
                Integer.class, studentId, examId);

        insertAttempt(LocalDateTime.now().minusDays(10), 1);
        attemptId = insertAttempt(LocalDateTime.now().minusHours(1), 1);

        insertAnswer("Security and Compliance", "Which IAM entity should an application on EC2 use?", false);
        insertAnswer("Security and Compliance", "Who patches the guest OS on EC2?", false);
        insertAnswer("Cloud Technology and Services", "Which S3 class fits rarely accessed data?", false);
        insertAnswer("Cloud Concepts", "What is elasticity?", true);

        when(deepSeekClient.model()).thenReturn("deepseek-flash");

        AuthenticatedUser principal = new AuthenticatedUser(saved);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("""
                DELETE FROM attempt_feedback WHERE attempt_id IN
                    (SELECT id FROM student_practice_attempts WHERE practice_exam_id = ?)
                """, practiceExamId);
        jdbcTemplate.update("""
                DELETE FROM student_answers WHERE student_practice_attempt_id IN
                    (SELECT id FROM student_practice_attempts WHERE practice_exam_id = ?)
                """, practiceExamId);
        jdbcTemplate.update("DELETE FROM student_practice_attempts WHERE practice_exam_id = ?", practiceExamId);
        jdbcTemplate.update("DELETE FROM enrollments WHERE student_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM alternatives WHERE question_id IN (SELECT id FROM questions WHERE practice_exam_id = ?)",
                practiceExamId);
        jdbcTemplate.update("DELETE FROM questions WHERE practice_exam_id = ?", practiceExamId);
        jdbcTemplate.update("DELETE FROM practice_exams WHERE id = ?", practiceExamId);
        jdbcTemplate.update("DELETE FROM exams WHERE id = ?", examId);
        jdbcTemplate.update("DELETE FROM subscriptions WHERE user_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", studentId);
    }

    @Test
    @DisplayName("Should store a READY plan when the model answers with a valid plan")
    void shouldStoreAReadyPlanOnSuccess() throws Exception {
        givenTheModelAnswers(validPlan());

        generateNow();

        Map<String, Object> row = feedbackRow();
        assertEquals("READY", row.get("status"));
        assertEquals(1, row.get("attempts"));
        assertNull(row.get("failure_reason"));
        assertEquals("deepseek-flash", row.get("model"));
        assertEquals("en", row.get("language"));
        assertEquals(64, ((String) row.get("prompt_hash")).length());
        assertEquals(3, objectMapper.readTree((String) row.get("content")).get("priorities").size());
    }

    @Test
    @DisplayName("Should send the stats and missed question text but never answer options or correct answers")
    void shouldNeverSendAnswerOptions() throws Exception {
        givenTheModelAnswers(validPlan());

        generateNow();

        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(deepSeekClient).completeJson(systemPrompt.capture(), userPrompt.capture(), eq(StudyPlan.class));

        JsonNode stats = objectMapper.readTree(userPrompt.getValue());
        assertEquals(1, stats.get("correct").asInt());
        assertEquals(4, stats.get("total").asInt());
        assertEquals("Security and Compliance", stats.get("domains").get(0).get("domain").asText());
        assertEquals(3, stats.get("missedQuestions").size());
        assertEquals(1, stats.get("previousScores").size());
        assertTrue(userPrompt.getValue().contains("Which IAM entity should an application on EC2 use?"));
        assertFalse(userPrompt.getValue().contains("What is elasticity?"));
        assertFalse(userPrompt.getValue().contains("Option"));
        assertFalse(userPrompt.getValue().contains("isCorrect"));
        assertTrue(systemPrompt.getValue().contains("in English"));
    }

    @Test
    @DisplayName("Should put the weakest domain's missed questions first when the cap cuts the list")
    void shouldSendTheWeakestDomainsMissedQuestionsFirst() throws Exception {
        attemptId = insertAttempt(LocalDateTime.now().minusMinutes(30), 1);
        for (int i = 0; i < 31; i++) {
            insertAnswer("Cloud Technology and Services", "Storage question %d".formatted(i), i == 0);
        }
        insertAnswer("Security and Compliance", "IAM question A", false);
        insertAnswer("Security and Compliance", "IAM question B", false);
        givenTheModelAnswers(validPlan());

        generateNow();

        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(deepSeekClient).completeJson(anyString(), userPrompt.capture(), eq(StudyPlan.class));

        JsonNode missed = objectMapper.readTree(userPrompt.getValue()).get("missedQuestions");
        assertEquals(30, missed.size());
        assertEquals("Security and Compliance", missed.get(0).get("domain").asText());
        assertEquals("Security and Compliance", missed.get(1).get("domain").asText());
        assertEquals("Cloud Technology and Services", missed.get(2).get("domain").asText());
    }

    @Test
    @DisplayName("Should record a FAILED attempt with the reason when the model call fails")
    void shouldRecordAFailedAttemptWhenTheCallFails() {
        when(deepSeekClient.completeJson(anyString(), anyString(), eq(StudyPlan.class)))
                .thenThrow(new IllegalStateException("DeepSeek call failed after 2 attempts",
                        new IllegalStateException("DeepSeek returned an invalid StudyPlan: plan size must be between 5 and 7")));

        generateNow();

        Map<String, Object> row = feedbackRow();
        assertEquals("FAILED", row.get("status"));
        assertEquals(1, row.get("attempts"));
        assertNull(row.get("content"));
        assertTrue(((String) row.get("failure_reason")).contains("plan size must be between 5 and 7"));
    }

    @Test
    @DisplayName("Should retry a FAILED attempt and store the plan when the next call succeeds")
    void shouldRetryAFailedAttempt() throws Exception {
        when(deepSeekClient.completeJson(anyString(), anyString(), eq(StudyPlan.class)))
                .thenThrow(new IllegalStateException("DeepSeek call failed after 2 attempts"))
                .thenReturn(validPlan());

        generateNow();
        generateNow();

        Map<String, Object> row = feedbackRow();
        assertEquals("READY", row.get("status"));
        assertEquals(2, row.get("attempts"));
        assertNull(row.get("failure_reason"));
        assertNotNull(row.get("content"));
    }

    @Test
    @DisplayName("Should stop calling the model after three failed attempts")
    void shouldStopCallingTheModelAfterThreeFailures() {
        when(deepSeekClient.completeJson(anyString(), anyString(), eq(StudyPlan.class)))
                .thenThrow(new IllegalStateException("DeepSeek call failed after 2 attempts"));

        generateNow();
        generateNow();
        generateNow();
        generateNow();

        verify(deepSeekClient, times(3)).completeJson(anyString(), anyString(), eq(StudyPlan.class));
        assertEquals(3, feedbackRow().get("attempts"));
    }

    @Test
    @DisplayName("Should fail the attempt when a priority names a domain that is not in the attempt")
    void shouldFailForAnUnknownPriorityDomain() throws Exception {
        StudyPlan valid = validPlan();
        givenTheModelAnswers(new StudyPlan(valid.summary(), List.of(
                new StudyPlan.Priority("Security & Compliance", "Missed IAM.", "IAM policies."),
                valid.priorities().get(1),
                valid.priorities().get(2)), valid.plan()));

        generateNow();

        Map<String, Object> row = feedbackRow();
        assertEquals("FAILED", row.get("status"));
        assertTrue(((String) row.get("failure_reason")).contains("Security & Compliance"));
    }

    @Test
    @DisplayName("Should fail the attempt when a priority domain is repeated")
    void shouldFailForARepeatedPriorityDomain() throws Exception {
        StudyPlan valid = validPlan();
        givenTheModelAnswers(new StudyPlan(valid.summary(), List.of(
                valid.priorities().get(0), valid.priorities().get(0), valid.priorities().get(1)), valid.plan()));

        generateNow();

        assertEquals("FAILED", feedbackRow().get("status"));
    }

    @Test
    @DisplayName("Should fail the attempt when the plan has fewer priorities than the attempt has domains")
    void shouldFailForTheWrongNumberOfPriorities() throws Exception {
        StudyPlan valid = validPlan();
        givenTheModelAnswers(new StudyPlan(valid.summary(), valid.priorities().subList(0, 2), valid.plan()));

        generateNow();

        assertEquals("FAILED", feedbackRow().get("status"));
    }

    @Test
    @DisplayName("Should fail the attempt when the plan contains a link")
    void shouldFailForAPlanWithALink() throws Exception {
        StudyPlan valid = validPlan();
        givenTheModelAnswers(new StudyPlan("Read https://example.com first. Then practice.", valid.priorities(), valid.plan()));

        generateNow();

        assertEquals("FAILED", feedbackRow().get("status"));
    }

    @Test
    @DisplayName("Should generate the plan in the background after the first pending request and serve it afterwards")
    void shouldGenerateThePlanFromThePendingRequest() throws Exception {
        insertSubscription(OffsetDateTime.now().plusDays(30));
        givenTheModelAnswers(validPlan());

        ResponseEntity<GetAttemptFeedbackResponse> first = controller.getFeedback(attemptId);

        assertEquals(HttpStatus.ACCEPTED, first.getStatusCode());
        waitFor(() -> "READY".equals(statusOrNull()));

        ResponseEntity<GetAttemptFeedbackResponse> second = controller.getFeedback(attemptId);

        assertEquals(HttpStatus.OK, second.getStatusCode());
        assertNotNull(second.getBody());
        assertEquals(GetAttemptFeedbackResponse.READY, second.getBody().status());
        verify(deepSeekClient, times(1)).completeJson(anyString(), anyString(), eq(StudyPlan.class));
    }

    @Test
    @DisplayName("Should keep answering 202 pending and retry while a FAILED attempt has retries left")
    void shouldRetryWhileAttemptsRemain() {
        insertSubscription(OffsetDateTime.now().plusDays(30));
        insertFeedback("FAILED", 1, null);
        when(deepSeekClient.completeJson(anyString(), anyString(), eq(StudyPlan.class)))
                .thenThrow(new IllegalStateException("DeepSeek call failed after 2 attempts"));

        ResponseEntity<GetAttemptFeedbackResponse> response = controller.getFeedback(attemptId);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(GetAttemptFeedbackResponse.PENDING, response.getBody().status());
        waitFor(() -> attempts() == 2);
    }

    @Test
    @DisplayName("Should answer failed, without calling the model, once the attempts are used up")
    void shouldAnswerFailedOnceTheAttemptsAreUsedUp() {
        insertSubscription(OffsetDateTime.now().plusDays(30));
        insertFeedback("FAILED", AttemptFeedbackService.MAX_ATTEMPTS, null);

        ResponseEntity<GetAttemptFeedbackResponse> response = controller.getFeedback(attemptId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(GetAttemptFeedbackResponse.FAILED, response.getBody().status());
        assertNull(response.getBody().content());
        verify(deepSeekClient, after(500).never()).completeJson(anyString(), anyString(), eq(StudyPlan.class));
    }

    @Test
    @DisplayName("Should return only the weakest domain, locked, to a free student")
    void shouldLockThePlanForAFreeStudent() {
        insertFeedback("READY", 1, VALID_PLAN);

        ResponseEntity<GetAttemptFeedbackResponse> response = controller.getFeedback(attemptId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        GetAttemptFeedbackResponse body = response.getBody();
        assertNotNull(body);
        assertTrue(body.locked());
        assertEquals("Security and Compliance", body.weakestDomain());
        assertNull(body.content());
        assertNull(body.status());
        verify(deepSeekClient, never()).completeJson(anyString(), anyString(), eq(StudyPlan.class));
    }

    @Test
    @DisplayName("Should return the stored plan to a premium student")
    void shouldReturnTheStoredPlanToAPremiumStudent() {
        insertSubscription(OffsetDateTime.now().plusDays(30));
        insertFeedback("READY", 1, VALID_PLAN);

        ResponseEntity<GetAttemptFeedbackResponse> response = controller.getFeedback(attemptId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        GetAttemptFeedbackResponse body = response.getBody();
        assertNotNull(body);
        assertFalse(body.locked());
        assertEquals(GetAttemptFeedbackResponse.READY, body.status());
        assertEquals(5, body.content().plan().size());
        verify(deepSeekClient, never()).completeJson(anyString(), anyString(), eq(StudyPlan.class));
    }

    private void givenTheModelAnswers(StudyPlan plan) {
        when(deepSeekClient.completeJson(anyString(), anyString(), eq(StudyPlan.class))).thenReturn(plan);
    }

    private StudyPlan validPlan() throws Exception {
        return objectMapper.readValue(VALID_PLAN, StudyPlan.class);
    }

    private void generateNow() {
        AttemptFeedbackService target = AopTestUtils.getUltimateTargetObject(attemptFeedbackService);
        target.generate(attemptId);
    }

    private void waitFor(BooleanSupplier condition) {
        for (int i = 0; i < 100; i++) {
            if (condition.getAsBoolean()) return;
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("interrupted while waiting");
            }
        }
        fail("condition not met within 5 seconds");
    }

    private Integer insertAttempt(LocalDateTime startedAt, int score) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO student_practice_attempts (enrollment_id, practice_exam_id, status, start_time, end_time,
                                                       score, passed, language)
                VALUES (?, ?, 2, ?, ?, ?, false, 'en') RETURNING id
                """, Integer.class, enrollmentId, practiceExamId, startedAt, startedAt.plusMinutes(30), score);
    }

    private void insertAnswer(String domain, String content, boolean correct) {
        Integer questionId = jdbcTemplate.queryForObject("""
                INSERT INTO questions (practice_exam_id, question_type_id, content, points, version, is_active, language, domain)
                VALUES (?, 3, ?, 1, 1, true, 'en', ?) RETURNING id
                """, Integer.class, practiceExamId, "<p>" + content + "</p>", domain);
        jdbcTemplate.update("""
                INSERT INTO alternatives (question_id, content, is_correct, version, is_active)
                VALUES (?, 'Option right', true, 1, true), (?, 'Option wrong', false, 1, true)
                """, questionId, questionId);
        jdbcTemplate.update("""
                INSERT INTO student_answers (student_practice_attempt_id, question_id, question_version, is_correct)
                VALUES (?, ?, 1, ?)
                """, attemptId, questionId, correct);
    }

    private void insertFeedback(String status, int attempts, String content) {
        jdbcTemplate.update("""
                INSERT INTO attempt_feedback (attempt_id, language, model, content, prompt_hash, status, attempts)
                VALUES (?, 'en', 'deepseek-flash', ?::jsonb, ?, ?, ?)
                """, attemptId, content, "0".repeat(64), status, attempts);
    }

    private void insertSubscription(OffsetDateTime currentPeriodEnd) {
        jdbcTemplate.update("""
                INSERT INTO subscriptions (user_id, provider, external_subscription_id, status,
                                           current_period_end, cancel_at_period_end)
                VALUES (?, 'STRIPE', ?, 'ACTIVE', ?, false)
                """, studentId, "sub_test_%s".formatted(System.nanoTime()), currentPeriodEnd);
    }

    private Map<String, Object> feedbackRow() {
        return jdbcTemplate.queryForMap("""
                SELECT status, attempts, model, language, prompt_hash, content::text AS content, failure_reason
                FROM attempt_feedback WHERE attempt_id = ?
                """, attemptId);
    }

    private int attempts() {
        Integer attempts = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(attempts), 0) FROM attempt_feedback WHERE attempt_id = ?", Integer.class, attemptId);
        return attempts == null ? 0 : attempts;
    }

    private String statusOrNull() {
        List<String> statuses = jdbcTemplate.queryForList(
                "SELECT status FROM attempt_feedback WHERE attempt_id = ?", String.class, attemptId);
        return statuses.isEmpty() ? null : statuses.getFirst();
    }
}

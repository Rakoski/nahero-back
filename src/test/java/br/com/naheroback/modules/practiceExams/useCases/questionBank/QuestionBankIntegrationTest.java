package br.com.naheroback.modules.practiceExams.useCases.questionBank;

import br.com.naheroback.common.exceptions.custom.BadRequestException;
import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.common.exceptions.custom.PaymentRequiredException;
import br.com.naheroback.modules.auth.entities.AuthenticatedUser;
import br.com.naheroback.modules.practiceExams.DomainFixtures;
import br.com.naheroback.modules.practiceExams.entities.enums.QuestionBankStatus;
import br.com.naheroback.modules.practiceExams.useCases.questionBank.answer.AnswerQuestionBankQuestionRequest;
import br.com.naheroback.modules.practiceExams.useCases.questionBank.answer.AnswerQuestionBankQuestionResponse;
import br.com.naheroback.modules.practiceExams.useCases.questionBank.answer.AnswerQuestionBankQuestionUseCase;
import br.com.naheroback.modules.practiceExams.useCases.questionBank.get.GetQuestionBankQuestionResponse;
import br.com.naheroback.modules.practiceExams.useCases.questionBank.get.GetQuestionBankQuestionUseCase;
import br.com.naheroback.modules.practiceExams.useCases.questionBank.getFilters.GetQuestionBankFiltersResponse;
import br.com.naheroback.modules.practiceExams.useCases.questionBank.getFilters.GetQuestionBankFiltersUseCase;
import br.com.naheroback.modules.practiceExams.useCases.questionBank.list.ListQuestionBankRequest;
import br.com.naheroback.modules.practiceExams.useCases.questionBank.list.ListQuestionBankResponse;
import br.com.naheroback.modules.practiceExams.useCases.questionBank.list.ListQuestionBankUseCase;
import br.com.naheroback.modules.user.entities.Role;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.entities.enums.RolesEnum;
import br.com.naheroback.modules.user.repositories.RoleRepository;
import br.com.naheroback.modules.user.repositories.UserRepository;
import br.com.naheroback.providers.turnstile.TurnstileClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "reengagement.enabled=false",
        "payment.reconcile.enabled=false"
})
class QuestionBankIntegrationTest {

    private static final String CONCEPTS = "Cloud Concepts";
    private static final String SECURITY = "Security and Compliance";

    @Autowired
    private GetQuestionBankFiltersUseCase getFilters;

    @Autowired
    private ListQuestionBankUseCase listQuestionBank;

    @Autowired
    private GetQuestionBankQuestionUseCase getQuestion;

    @Autowired
    private AnswerQuestionBankQuestionUseCase answerQuestion;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private TurnstileClient turnstileClient;

    private final List<Integer> userIds = new ArrayList<>();
    private final List<Integer> examIds = new ArrayList<>();
    private final List<Integer> practiceExamIds = new ArrayList<>();

    private Integer studentId;
    private Integer practiceExamId;
    private Integer conceptsDomainId;
    private Integer securityDomainId;
    private Integer answeredInAttempt;
    private Integer skippedInAttempt;
    private Integer untouched;
    private Integer otherDomainQuestion;
    private Integer otherExamQuestion;
    private Integer portugueseQuestion;
    private Integer descriptiveQuestion;
    private Integer inactiveQuestion;

    @BeforeEach
    void setUp() {
        studentId = createStudent();
        insertSubscription(studentId);

        practiceExamId = createPracticeExam("question-bank-" + System.nanoTime());
        Integer otherPracticeExamId = createPracticeExam("question-bank-other-" + System.nanoTime());
        conceptsDomainId = DomainFixtures.link(jdbcTemplate, practiceExamId, CONCEPTS);
        securityDomainId = DomainFixtures.link(jdbcTemplate, practiceExamId, SECURITY);
        DomainFixtures.link(jdbcTemplate, otherPracticeExamId, CONCEPTS);

        answeredInAttempt = insertQuestion(practiceExamId, conceptsDomainId, "<p>Answered in an attempt</p>", "en", 3, true);
        skippedInAttempt = insertQuestion(practiceExamId, conceptsDomainId, "Skipped in an attempt", "en", 3, true);
        untouched = insertQuestion(practiceExamId, conceptsDomainId, "Never shown", "en", 3, true);
        otherDomainQuestion = insertQuestion(practiceExamId, securityDomainId, "Security question", "en", 1, true);
        portugueseQuestion = insertQuestion(practiceExamId, conceptsDomainId, "Questão em português", "pt", 3, true);
        descriptiveQuestion = insertQuestion(practiceExamId, conceptsDomainId, "Explain elasticity", "en", 4, true);
        inactiveQuestion = insertQuestion(practiceExamId, conceptsDomainId, "Retired question", "en", 3, false);
        otherExamQuestion = insertQuestion(otherPracticeExamId, conceptsDomainId, "Other exam", "en", 3, true);

        Integer enrollmentId = jdbcTemplate.queryForObject("""
                INSERT INTO enrollments (student_id, exam_id)
                VALUES (?, (SELECT exam_id FROM practice_exams WHERE id = ?)) RETURNING id
                """, Integer.class, studentId, practiceExamId);
        Integer attemptId = jdbcTemplate.queryForObject("""
                INSERT INTO student_practice_attempts (enrollment_id, practice_exam_id, status, start_time, end_time,
                                                       score, passed, language)
                VALUES (?, ?, 2, ?, ?, 1, false, 'en') RETURNING id
                """, Integer.class, enrollmentId, practiceExamId,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().minusMinutes(30));
        jdbcTemplate.update("""
                INSERT INTO student_answers (student_practice_attempt_id, question_id, question_version,
                                             selected_alternative_id, selected_alternative_version, is_correct)
                VALUES (?, ?, 1, ?, 1, true)
                """, attemptId, answeredInAttempt, correctAlternativeOf(answeredInAttempt));
        jdbcTemplate.update("""
                INSERT INTO student_answers (student_practice_attempt_id, question_id, question_version, is_correct)
                VALUES (?, ?, 1, false)
                """, attemptId, skippedInAttempt);

        authenticateAs(studentId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        for (Integer id : userIds) {
            jdbcTemplate.update("DELETE FROM question_study_answers WHERE student_id = ?", id);
            jdbcTemplate.update("""
                    DELETE FROM student_answers WHERE student_practice_attempt_id IN
                        (SELECT a.id FROM student_practice_attempts a JOIN enrollments e ON e.id = a.enrollment_id
                         WHERE e.student_id = ?)
                    """, id);
            jdbcTemplate.update("""
                    DELETE FROM student_practice_attempts WHERE enrollment_id IN
                        (SELECT id FROM enrollments WHERE student_id = ?)
                    """, id);
            jdbcTemplate.update("DELETE FROM enrollments WHERE student_id = ?", id);
            jdbcTemplate.update("DELETE FROM subscriptions WHERE user_id = ?", id);
            jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", id);
        }
        for (Integer id : practiceExamIds) {
            jdbcTemplate.update("DELETE FROM alternatives WHERE question_id IN (SELECT id FROM questions WHERE practice_exam_id = ?)", id);
            jdbcTemplate.update("DELETE FROM questions WHERE practice_exam_id = ?", id);
            DomainFixtures.unlinkAll(jdbcTemplate, id);
            jdbcTemplate.update("DELETE FROM practice_exams WHERE id = ?", id);
        }
        examIds.forEach(id -> jdbcTemplate.update("DELETE FROM exams WHERE id = ?", id));
        userIds.forEach(id -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", id));
    }

    @Test
    @DisplayName("Should keep the whole question bank behind Premium")
    void shouldRequirePremium() {
        authenticateAs(createStudent());

        assertThrows(PaymentRequiredException.class, () -> getFilters.execute());
        assertThrows(PaymentRequiredException.class, () -> list(null, null, null));
        assertThrows(PaymentRequiredException.class, () -> getQuestion.execute(untouched, null));
        assertThrows(PaymentRequiredException.class, () -> answerQuestion.execute(
                untouched, new AnswerQuestionBankQuestionRequest(List.of(correctAlternativeOf(untouched))), null));
    }

    @Test
    @DisplayName("Should list only active choice questions in the student's language for the chosen exam")
    void shouldListVisibleQuestionsOfExam() {
        List<Integer> ids = allIds(null, null);

        assertEquals(List.of(answeredInAttempt, skippedInAttempt, untouched, otherDomainQuestion), ids);
        assertFalse(ids.contains(portugueseQuestion));
        assertFalse(ids.contains(descriptiveQuestion));
        assertFalse(ids.contains(inactiveQuestion));
        assertFalse(ids.contains(otherExamQuestion));
    }

    @Test
    @DisplayName("Should narrow the list to one domain")
    void shouldFilterByDomain() {
        assertEquals(List.of(otherDomainQuestion), allIds(securityDomainId, null));
    }

    @Test
    @DisplayName("Should treat a question as resolved only when the student actually picked an answer")
    void shouldSplitResolvedFromUnresolved() {
        assertEquals(List.of(answeredInAttempt), allIds(null, QuestionBankStatus.RESOLVED));
        assertEquals(List.of(skippedInAttempt, untouched, otherDomainQuestion), allIds(null, QuestionBankStatus.UNRESOLVED));
    }

    @Test
    @DisplayName("Should page without a total, flagging the next page and capping the page size")
    void shouldPageWithoutTotal() {
        ListQuestionBankResponse first = listQuestionBank.execute(
                new ListQuestionBankRequest(practiceExamId, null, null, 0, 3), Locale.ENGLISH, null);
        ListQuestionBankResponse second = listQuestionBank.execute(
                new ListQuestionBankRequest(practiceExamId, null, null, 1, 3), Locale.ENGLISH, null);
        ListQuestionBankResponse oversized = listQuestionBank.execute(
                new ListQuestionBankRequest(practiceExamId, null, null, 0, 500), Locale.ENGLISH, null);

        assertEquals(3, first.items().size());
        assertTrue(first.hasNext());
        assertEquals(List.of(otherDomainQuestion), second.items().stream().map(ListQuestionBankResponse.Item::id).toList());
        assertFalse(second.hasNext());
        assertEquals(20, oversized.size());
    }

    @Test
    @DisplayName("Should show a plain-text preview, the domain and the answered flag in each row")
    void shouldDescribeEachRow() {
        ListQuestionBankResponse.Item row = list(null, null, 0).items().getFirst();

        assertEquals(answeredInAttempt, row.id());
        assertEquals("Answered in an attempt", row.preview());
        assertEquals(new ListQuestionBankResponse.DomainSummary(conceptsDomainId, CONCEPTS), row.domain());
        assertEquals(practiceExamId, row.practiceExam().id());
        assertTrue(row.answered());
    }

    @Test
    @DisplayName("Should hand out the question with its alternatives but without revealing which is correct")
    void shouldGetQuestionWithoutAnswer() {
        GetQuestionBankQuestionResponse question = getQuestion.execute(untouched, null);

        assertEquals("Never shown", question.content());
        assertEquals(2, question.alternatives().size());
        assertFalse(question.answered());
        assertThrows(NotFoundException.class, () -> getQuestion.execute(descriptiveQuestion, null));
        assertThrows(NotFoundException.class, () -> getQuestion.execute(inactiveQuestion, null));
    }

    @Test
    @DisplayName("Should grade, store the answer and move the question to resolved")
    void shouldStoreStudyAnswer() {
        Integer wrong = jdbcTemplate.queryForObject(
                "SELECT id FROM alternatives WHERE question_id = ? AND is_correct = false", Integer.class, untouched);

        AnswerQuestionBankQuestionResponse response = answerQuestion.execute(
                untouched, new AnswerQuestionBankQuestionRequest(List.of(wrong)), null);

        assertFalse(response.correct());
        assertEquals(List.of(correctAlternativeOf(untouched)), response.correctAlternativeIds());
        assertEquals("Because Never shown", response.explanation());

        Map<String, Object> stored = jdbcTemplate.queryForMap(
                "SELECT selected_alternative_ids, is_correct FROM question_study_answers WHERE student_id = ? AND question_id = ?",
                studentId, untouched);
        assertEquals(String.valueOf(wrong), stored.get("selected_alternative_ids"));
        assertEquals(false, stored.get("is_correct"));
        assertEquals(List.of(answeredInAttempt, untouched), allIds(null, QuestionBankStatus.RESOLVED));
    }

    @Test
    @DisplayName("Should refuse alternatives that belong to another question")
    void shouldRejectForeignAlternative() {
        assertThrows(BadRequestException.class, () -> answerQuestion.execute(
                untouched, new AnswerQuestionBankQuestionRequest(List.of(correctAlternativeOf(otherDomainQuestion))), null));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM question_study_answers WHERE student_id = ?", Integer.class, studentId));
    }

    @Test
    @DisplayName("Should offer each active exam with its domains as filters")
    void shouldListFilters() {
        GetQuestionBankFiltersResponse exam = getFilters.execute().stream()
                .filter(filter -> filter.practiceExamId().equals(practiceExamId))
                .findFirst()
                .orElseThrow();

        assertEquals(Set.of(CONCEPTS, SECURITY), new HashSet<>(exam.domains().stream()
                .map(GetQuestionBankFiltersResponse.DomainOption::name).toList()));
    }

    private ListQuestionBankResponse list(Integer domainId, QuestionBankStatus status, Integer page) {
        return listQuestionBank.execute(
                new ListQuestionBankRequest(practiceExamId, domainId, status, page, null), Locale.ENGLISH, null);
    }

    private List<Integer> allIds(Integer domainId, QuestionBankStatus status) {
        List<Integer> ids = new ArrayList<>();
        int page = 0;
        ListQuestionBankResponse response;
        do {
            response = list(domainId, status, page++);
            response.items().forEach(item -> ids.add(item.id()));
        } while (response.hasNext());
        return ids;
    }

    private Integer createStudent() {
        Role student = roleRepository.findByName(RolesEnum.IS_STUDENT.name()).orElseThrow();
        User user = new User();
        user.setName("Question Bank Student");
        user.setEmail("question-bank-%s@example.com".formatted(System.nanoTime()));
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

    private void insertSubscription(Integer userId) {
        jdbcTemplate.update("""
                INSERT INTO subscriptions (user_id, provider, external_subscription_id, status,
                                           current_period_end, cancel_at_period_end)
                VALUES (?, 'STRIPE', ?, 'ACTIVE', ?, false)
                """, userId, "sub_test_%s".formatted(System.nanoTime()), OffsetDateTime.now().plusDays(30));
    }

    private Integer createPracticeExam(String slug) {
        Integer examId = jdbcTemplate.queryForObject("""
                INSERT INTO exams (title, difficulty_level, is_active) VALUES (?, 2, true) RETURNING id
                """, Integer.class, slug + " Exam");
        examIds.add(examId);
        Integer id = jdbcTemplate.queryForObject("""
                INSERT INTO practice_exams (exam_id, title, slug, passing_score, time_limit, number_of_questions, is_active)
                VALUES (?, ?, ?, 50, 60, 2, true) RETURNING id
                """, Integer.class, examId, slug, slug);
        practiceExamIds.add(id);
        return id;
    }

    private Integer insertQuestion(Integer practiceExamId, Integer domainId, String content, String language,
                                   int typeId, boolean active) {
        Integer questionId = jdbcTemplate.queryForObject("""
                INSERT INTO questions (practice_exam_id, question_type_id, content, explanation, points, version,
                                       is_active, language, domain_id)
                VALUES (?, ?, ?, ?, 1, 1, ?, ?, ?) RETURNING id
                """, Integer.class, practiceExamId, typeId, content, "Because " + content, active, language, domainId);
        jdbcTemplate.update("""
                INSERT INTO alternatives (question_id, content, is_correct, version, is_active)
                VALUES (?, 'Right', true, 1, true), (?, 'Wrong', false, 1, true)
                """, questionId, questionId);
        return questionId;
    }

    private Integer correctAlternativeOf(Integer questionId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM alternatives WHERE question_id = ? AND is_correct = true", Integer.class, questionId);
    }
}

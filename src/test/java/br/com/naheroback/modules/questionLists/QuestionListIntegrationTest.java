package br.com.naheroback.modules.questionLists;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.common.exceptions.custom.PaymentRequiredException;
import br.com.naheroback.common.exceptions.custom.UnprocessableEntityException;
import br.com.naheroback.modules.auth.entities.AuthenticatedUser;
import br.com.naheroback.modules.practiceExams.DomainFixtures;
import br.com.naheroback.modules.practiceExams.entities.enums.QuestionBankStatus;
import br.com.naheroback.modules.questionLists.useCases.addQuestions.AddQuestionListQuestionsRequest;
import br.com.naheroback.modules.questionLists.useCases.addQuestions.AddQuestionListQuestionsResponse;
import br.com.naheroback.modules.questionLists.useCases.addQuestions.AddQuestionListQuestionsUseCase;
import br.com.naheroback.modules.questionLists.useCases.answer.AnswerQuestionListQuestionRequest;
import br.com.naheroback.modules.questionLists.useCases.answer.AnswerQuestionListQuestionResponse;
import br.com.naheroback.modules.questionLists.useCases.answer.AnswerQuestionListQuestionUseCase;
import br.com.naheroback.modules.questionLists.useCases.create.CreateQuestionListRequest;
import br.com.naheroback.modules.questionLists.useCases.create.CreateQuestionListResponse;
import br.com.naheroback.modules.questionLists.useCases.create.CreateQuestionListUseCase;
import br.com.naheroback.modules.questionLists.useCases.delete.DeleteQuestionListUseCase;
import br.com.naheroback.modules.questionLists.useCases.get.GetQuestionListResponse;
import br.com.naheroback.modules.questionLists.useCases.get.GetQuestionListUseCase;
import br.com.naheroback.modules.questionLists.useCases.listMine.ListMyQuestionListsResponse;
import br.com.naheroback.modules.questionLists.useCases.listMine.ListMyQuestionListsUseCase;
import br.com.naheroback.modules.questionLists.useCases.removeQuestion.RemoveQuestionListQuestionUseCase;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "reengagement.enabled=false",
        "payment.reconcile.enabled=false"
})
class QuestionListIntegrationTest {

    @Autowired
    private CreateQuestionListUseCase createList;

    @Autowired
    private ListMyQuestionListsUseCase listMine;

    @Autowired
    private GetQuestionListUseCase getList;

    @Autowired
    private DeleteQuestionListUseCase deleteList;

    @Autowired
    private AddQuestionListQuestionsUseCase addQuestions;

    @Autowired
    private RemoveQuestionListQuestionUseCase removeQuestion;

    @Autowired
    private AnswerQuestionListQuestionUseCase answerQuestion;

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
    private Integer firstExamId;
    private Integer secondExamId;
    private Integer conceptsDomainId;
    private Integer securityDomainId;
    private Integer resolvedConcepts;
    private Integer unresolvedConceptsA;
    private Integer unresolvedConceptsB;
    private Integer security;
    private Integer otherExamConcepts;
    private Integer portuguese;
    private Integer descriptive;

    @BeforeEach
    void setUp() {
        studentId = createStudent(true);

        firstExamId = createPracticeExam("question-list-" + System.nanoTime());
        secondExamId = createPracticeExam("question-list-other-" + System.nanoTime());
        String concepts = "List Concepts " + System.nanoTime();
        conceptsDomainId = DomainFixtures.link(jdbcTemplate, firstExamId, concepts);
        securityDomainId = DomainFixtures.link(jdbcTemplate, firstExamId, "List Security " + System.nanoTime());
        DomainFixtures.link(jdbcTemplate, secondExamId, concepts);

        resolvedConcepts = insertQuestion(firstExamId, conceptsDomainId, "en", 3);
        unresolvedConceptsA = insertQuestion(firstExamId, conceptsDomainId, "en", 3);
        unresolvedConceptsB = insertQuestion(firstExamId, conceptsDomainId, "en", 3);
        security = insertQuestion(firstExamId, securityDomainId, "en", 3);
        otherExamConcepts = insertQuestion(secondExamId, conceptsDomainId, "en", 3);
        portuguese = insertQuestion(firstExamId, conceptsDomainId, "pt", 3);
        descriptive = insertQuestion(firstExamId, conceptsDomainId, "en", 4);

        Integer enrollmentId = jdbcTemplate.queryForObject("""
                INSERT INTO enrollments (student_id, exam_id)
                VALUES (?, (SELECT exam_id FROM practice_exams WHERE id = ?)) RETURNING id
                """, Integer.class, studentId, firstExamId);
        Integer attemptId = jdbcTemplate.queryForObject("""
                INSERT INTO student_practice_attempts (enrollment_id, practice_exam_id, status, start_time, end_time,
                                                       score, passed, language)
                VALUES (?, ?, 2, ?, ?, 1, false, 'en') RETURNING id
                """, Integer.class, enrollmentId, firstExamId,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().minusMinutes(30));
        jdbcTemplate.update("""
                INSERT INTO student_answers (student_practice_attempt_id, question_id, question_version,
                                             selected_alternative_id, selected_alternative_version, is_correct)
                VALUES (?, ?, 1, ?, 1, true)
                """, attemptId, resolvedConcepts, correctAlternativeOf(resolvedConcepts));

        authenticateAs(studentId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        for (Integer id : userIds) {
            jdbcTemplate.update("DELETE FROM question_study_answers WHERE student_id = ?", id);
            jdbcTemplate.update("""
                    DELETE FROM question_list_items WHERE question_list_id IN
                        (SELECT id FROM question_lists WHERE student_id = ?)
                    """, id);
            jdbcTemplate.update("DELETE FROM question_lists WHERE student_id = ?", id);
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
    @DisplayName("Should keep custom lists behind Premium")
    void shouldRequirePremium() {
        authenticateAs(createStudent(false));

        assertThrows(PaymentRequiredException.class, () -> createList.execute(picked(security), Locale.ENGLISH, null));
        assertThrows(PaymentRequiredException.class, () -> listMine.execute());
    }

    @Test
    @DisplayName("Should keep hand-picked questions in the order the student chose them, across exams")
    void shouldCreateFromPickedQuestions() {
        CreateQuestionListResponse created = createList.execute(
                picked(security, otherExamConcepts, unresolvedConceptsA, security), Locale.ENGLISH, null);

        assertEquals(3, created.questionCount());
        assertEquals(List.of(security, otherExamConcepts, unresolvedConceptsA), itemIds(created.id()));
    }

    @Test
    @DisplayName("Should refuse hand-picked questions the bank does not offer and write nothing")
    void shouldRejectUnavailableQuestions() {
        assertThrows(UnprocessableEntityException.class,
                () -> createList.execute(picked(security, descriptive), Locale.ENGLISH, null));
        assertThrows(UnprocessableEntityException.class,
                () -> createList.execute(picked(Integer.MAX_VALUE), Locale.ENGLISH, null));

        assertEquals(0, countLists(studentId));
    }

    @Test
    @DisplayName("Should fill a list from a domain filter with only the unresolved questions in the student's language")
    void shouldCreateFromUnresolvedDomainFilter() {
        CreateQuestionListResponse created = createList.execute(new CreateQuestionListRequest(
                "Unresolved concepts", null,
                new CreateQuestionListRequest.Filter(firstExamId, conceptsDomainId, QuestionBankStatus.UNRESOLVED, null)),
                Locale.ENGLISH, null);

        assertEquals(Set.of(unresolvedConceptsA, unresolvedConceptsB), new HashSet<>(itemIds(created.id())));
    }

    @Test
    @DisplayName("Should draw a domain's questions across every exam that has it when no exam is chosen")
    void shouldFilterDomainAcrossExams() {
        CreateQuestionListResponse created = createList.execute(new CreateQuestionListRequest(
                "All concepts", null,
                new CreateQuestionListRequest.Filter(null, conceptsDomainId, null, null)),
                Locale.ENGLISH, null);

        assertEquals(Set.of(resolvedConcepts, unresolvedConceptsA, unresolvedConceptsB, otherExamConcepts),
                new HashSet<>(itemIds(created.id())));
    }

    @Test
    @DisplayName("Should put picked questions first, then sample the filter up to its limit without repeats")
    void shouldCombinePickedAndFilter() {
        CreateQuestionListResponse created = createList.execute(new CreateQuestionListRequest(
                "Mixed", List.of(unresolvedConceptsA),
                new CreateQuestionListRequest.Filter(firstExamId, conceptsDomainId, QuestionBankStatus.UNRESOLVED, 5)),
                Locale.ENGLISH, null);

        assertEquals(List.of(unresolvedConceptsA, unresolvedConceptsB), itemIds(created.id()));

        CreateQuestionListResponse limited = createList.execute(new CreateQuestionListRequest(
                "One only", null,
                new CreateQuestionListRequest.Filter(firstExamId, null, null, 1)),
                Locale.ENGLISH, null);
        assertEquals(1, limited.questionCount());
    }

    @Test
    @DisplayName("Should refuse a list that would be empty")
    void shouldRejectEmptyList() {
        assertThrows(UnprocessableEntityException.class, () -> createList.execute(new CreateQuestionListRequest(
                "Nothing", null,
                new CreateQuestionListRequest.Filter(firstExamId, securityDomainId, QuestionBankStatus.RESOLVED, null)),
                Locale.ENGLISH, null));
        assertThrows(UnprocessableEntityException.class, () -> createList.execute(
                new CreateQuestionListRequest("Nothing", List.of(), null), Locale.ENGLISH, null));
    }

    @Test
    @DisplayName("Should stop a student from keeping more lists than the cap")
    void shouldCapListsPerStudent() {
        jdbcTemplate.update("""
                INSERT INTO question_lists (student_id, name) SELECT ?, 'Filler ' || n FROM generate_series(1, 50) n
                """, studentId);

        assertThrows(UnprocessableEntityException.class,
                () -> createList.execute(picked(security), Locale.ENGLISH, null));
    }

    @Test
    @DisplayName("Should page a list and report how much of it the student has answered")
    void shouldGetListWithProgress() {
        Integer listId = createList.execute(
                picked(resolvedConcepts, unresolvedConceptsA, security), Locale.ENGLISH, null).id();

        GetQuestionListResponse first = getList.execute(listId, 0, 2, null);
        GetQuestionListResponse second = getList.execute(listId, 1, 2, null);

        assertEquals(3, first.questionCount());
        assertEquals(1, first.answeredCount());
        assertEquals(List.of(resolvedConcepts, unresolvedConceptsA),
                first.items().stream().map(GetQuestionListResponse.Item::questionId).toList());
        assertTrue(first.items().getFirst().answered());
        assertTrue(first.hasNext());
        assertEquals(List.of(security), second.items().stream().map(GetQuestionListResponse.Item::questionId).toList());
        assertFalse(second.hasNext());
    }

    @Test
    @DisplayName("Should drop a question from the list once it is retired from the bank")
    void shouldHideRetiredQuestions() {
        Integer listId = createList.execute(picked(unresolvedConceptsA, security), Locale.ENGLISH, null).id();
        jdbcTemplate.update("UPDATE questions SET is_active = false WHERE id = ?", security);

        assertEquals(List.of(unresolvedConceptsA), itemIds(listId));
        ListMyQuestionListsResponse summary = summaryOf(listId);
        assertEquals(1, summary.questionCount());
    }

    @Test
    @DisplayName("Should record a list answer against the list and count the question as answered")
    void shouldAnswerInsideList() {
        Integer listId = createList.execute(picked(unresolvedConceptsA), Locale.ENGLISH, null).id();

        AnswerQuestionListQuestionResponse response = answerQuestion.execute(listId, unresolvedConceptsA,
                new AnswerQuestionListQuestionRequest(List.of(correctAlternativeOf(unresolvedConceptsA))), null);

        assertTrue(response.correct());
        assertEquals(listId, jdbcTemplate.queryForObject(
                "SELECT question_list_id FROM question_study_answers WHERE student_id = ? AND question_id = ?",
                Integer.class, studentId, unresolvedConceptsA));
        assertEquals(1, summaryOf(listId).answeredCount());
        assertThrows(NotFoundException.class, () -> answerQuestion.execute(listId, security,
                new AnswerQuestionListQuestionRequest(List.of(correctAlternativeOf(security))), null));
    }

    @Test
    @DisplayName("Should add new questions to the end, skip ones already there, and remove on request")
    void shouldAddAndRemoveQuestions() {
        Integer listId = createList.execute(picked(security), Locale.ENGLISH, null).id();

        assertEquals(1, addQuestions.execute(listId,
                pickedToAdd(security, unresolvedConceptsB), Locale.ENGLISH, null).added());
        assertEquals(List.of(security, unresolvedConceptsB), itemIds(listId));

        removeQuestion.execute(listId, security);
        assertEquals(List.of(unresolvedConceptsB), itemIds(listId));
        assertThrows(UnprocessableEntityException.class, () -> addQuestions.execute(listId,
                pickedToAdd(portuguese, descriptive), Locale.ENGLISH, null));
        assertThrows(UnprocessableEntityException.class, () -> addQuestions.execute(listId,
                new AddQuestionListQuestionsRequest(List.of(), null), Locale.ENGLISH, null));
    }

    @Test
    @DisplayName("Should add every question matching a filter to an existing list, skipping ones already there")
    void shouldAddMatchingFilter() {
        Integer listId = createList.execute(picked(unresolvedConceptsA), Locale.ENGLISH, null).id();

        AddQuestionListQuestionsResponse response = addQuestions.execute(listId, new AddQuestionListQuestionsRequest(null,
                        new AddQuestionListQuestionsRequest.Filter(null, conceptsDomainId, QuestionBankStatus.UNRESOLVED)),
                Locale.ENGLISH, null);

        assertEquals(2, response.added());
        assertFalse(response.limitReached());
        assertEquals(List.of(unresolvedConceptsA, unresolvedConceptsB, otherExamConcepts), itemIds(listId));
    }

    @Test
    @DisplayName("Should fill a list up to its cap from a filter and say the cap was reached")
    void shouldStopFilterAtListCap() {
        Integer listId = createList.execute(picked(security), Locale.ENGLISH, null).id();
        for (int position = 1; position < 199; position++) {
            jdbcTemplate.update(
                    "INSERT INTO question_list_items (question_list_id, question_id, position) VALUES (?, ?, ?)",
                    listId, insertQuestion(secondExamId, null, "en", 3), position);
        }

        AddQuestionListQuestionsResponse response = addQuestions.execute(listId, new AddQuestionListQuestionsRequest(null,
                        new AddQuestionListQuestionsRequest.Filter(firstExamId, conceptsDomainId, null)),
                Locale.ENGLISH, null);

        assertEquals(1, response.added());
        assertTrue(response.limitReached());
        assertEquals(200, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM question_list_items WHERE question_list_id = ?", Integer.class, listId));
    }

    @Test
    @DisplayName("Should hide one student's list from another")
    void shouldHideListsFromOtherStudents() {
        Integer listId = createList.execute(picked(security), Locale.ENGLISH, null).id();
        authenticateAs(createStudent(true));

        assertThrows(NotFoundException.class, () -> getList.execute(listId, 0, null, null));
        assertThrows(NotFoundException.class, () -> deleteList.execute(listId));
        assertThrows(NotFoundException.class, () -> addQuestions.execute(listId,
                pickedToAdd(unresolvedConceptsA), Locale.ENGLISH, null));
        assertThrows(NotFoundException.class, () -> answerQuestion.execute(listId, security,
                new AnswerQuestionListQuestionRequest(List.of(correctAlternativeOf(security))), null));
        assertTrue(listMine.execute().isEmpty());
    }

    @Test
    @DisplayName("Should soft-delete a list so it no longer shows up")
    void shouldDeleteList() {
        Integer listId = createList.execute(picked(security), Locale.ENGLISH, null).id();

        deleteList.execute(listId);

        assertTrue(listMine.execute().isEmpty());
        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM question_lists WHERE id = ?", LocalDateTime.class, listId));
        assertThrows(NotFoundException.class, () -> getList.execute(listId, 0, null, null));
    }

    private CreateQuestionListRequest picked(Integer... questionIds) {
        return new CreateQuestionListRequest("Picked", List.of(questionIds), null);
    }

    private AddQuestionListQuestionsRequest pickedToAdd(Integer... questionIds) {
        return new AddQuestionListQuestionsRequest(List.of(questionIds), null);
    }

    private List<Integer> itemIds(Integer listId) {
        return getList.execute(listId, 0, null, null).items().stream()
                .map(GetQuestionListResponse.Item::questionId)
                .toList();
    }

    private ListMyQuestionListsResponse summaryOf(Integer listId) {
        return listMine.execute().stream().filter(list -> list.id().equals(listId)).findFirst().orElseThrow();
    }

    private int countLists(Integer userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM question_lists WHERE student_id = ?", Integer.class, userId);
        return count == null ? 0 : count;
    }

    private Integer createStudent(boolean premium) {
        Role student = roleRepository.findByName(RolesEnum.IS_STUDENT.name()).orElseThrow();
        User user = new User();
        user.setName("Question List Student");
        user.setEmail("question-list-%s@example.com".formatted(System.nanoTime()));
        user.setPassword("irrelevant");
        user.setRoles(new HashSet<>(Set.of(student)));
        Integer id = userRepository.save(user).getId();
        userIds.add(id);
        if (premium) {
            jdbcTemplate.update("""
                    INSERT INTO subscriptions (user_id, provider, external_subscription_id, status,
                                               current_period_end, cancel_at_period_end)
                    VALUES (?, 'STRIPE', ?, 'ACTIVE', ?, false)
                    """, id, "sub_test_%s".formatted(System.nanoTime()), OffsetDateTime.now().plusDays(30));
        }
        return id;
    }

    private void authenticateAs(Integer userId) {
        AuthenticatedUser principal = new AuthenticatedUser(userRepository.findById(userId).orElseThrow());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
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

    private Integer insertQuestion(Integer practiceExamId, Integer domainId, String language, int typeId) {
        Integer questionId = jdbcTemplate.queryForObject("""
                INSERT INTO questions (practice_exam_id, question_type_id, content, explanation, points, version,
                                       is_active, language, domain_id)
                VALUES (?, ?, 'Question', 'Because', 1, 1, true, ?, ?) RETURNING id
                """, Integer.class, practiceExamId, typeId, language, domainId);
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

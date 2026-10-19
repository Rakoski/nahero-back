package br.com.naheroback.modules.instructor;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.common.exceptions.custom.UnprocessableEntityException;
import br.com.naheroback.modules.auth.entities.AuthenticatedUser;
import br.com.naheroback.modules.exams.useCases.create.CreateExamRequest;
import br.com.naheroback.modules.exams.useCases.create.CreateExamUseCase;
import br.com.naheroback.modules.instructor.useCases.addDomains.AddPracticeExamDomainsRequest;
import br.com.naheroback.modules.instructor.useCases.addDomains.AddPracticeExamDomainsUseCase;
import br.com.naheroback.modules.instructor.useCases.createPracticeExam.CreateInstructorPracticeExamRequest;
import br.com.naheroback.modules.instructor.useCases.createPracticeExam.CreateInstructorPracticeExamResponse;
import br.com.naheroback.modules.instructor.useCases.createPracticeExam.CreateInstructorPracticeExamUseCase;
import br.com.naheroback.modules.instructor.useCases.createQuestions.CreateInstructorQuestionsRequest;
import br.com.naheroback.modules.instructor.useCases.createQuestions.CreateInstructorQuestionsResponse;
import br.com.naheroback.modules.instructor.useCases.createQuestions.CreateInstructorQuestionsUseCase;
import br.com.naheroback.modules.instructor.useCases.deleteQuestion.DeleteInstructorQuestionUseCase;
import br.com.naheroback.modules.instructor.useCases.getPracticeExam.GetInstructorPracticeExamResponse;
import br.com.naheroback.modules.instructor.useCases.getPracticeExam.GetInstructorPracticeExamUseCase;
import br.com.naheroback.modules.instructor.useCases.listExams.ListInstructorExamsResponse;
import br.com.naheroback.modules.instructor.useCases.listExams.ListInstructorExamsUseCase;
import br.com.naheroback.modules.instructor.useCases.listPracticeExams.ListInstructorPracticeExamsUseCase;
import br.com.naheroback.modules.instructor.useCases.listQuestions.ListInstructorQuestionsResponse;
import br.com.naheroback.modules.instructor.useCases.listQuestions.ListInstructorQuestionsUseCase;
import br.com.naheroback.modules.instructor.useCases.removeDomain.RemovePracticeExamDomainUseCase;
import br.com.naheroback.modules.instructor.useCases.updateStatus.UpdatePracticeExamStatusRequest;
import br.com.naheroback.modules.instructor.useCases.updateStatus.UpdatePracticeExamStatusUseCase;
import br.com.naheroback.modules.practiceExams.entities.enums.QuestionTypeEnum;
import br.com.naheroback.modules.practiceExams.useCases.practiceExam.getBySlug.GetPracticeExamBySlugUseCase;
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
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "reengagement.enabled=false",
        "payment.reconcile.enabled=false"
})
class InstructorAuthoringIntegrationTest {

    @Autowired
    private CreateExamUseCase createExam;

    @Autowired
    private ListInstructorExamsUseCase listExams;

    @Autowired
    private CreateInstructorPracticeExamUseCase createPracticeExam;

    @Autowired
    private ListInstructorPracticeExamsUseCase listPracticeExams;

    @Autowired
    private GetInstructorPracticeExamUseCase getPracticeExam;

    @Autowired
    private AddPracticeExamDomainsUseCase addDomains;

    @Autowired
    private RemovePracticeExamDomainUseCase removeDomain;

    @Autowired
    private CreateInstructorQuestionsUseCase createQuestions;

    @Autowired
    private ListInstructorQuestionsUseCase listQuestions;

    @Autowired
    private DeleteInstructorQuestionUseCase deleteQuestion;

    @Autowired
    private UpdatePracticeExamStatusUseCase updateStatus;

    @Autowired
    private GetPracticeExamBySlugUseCase getPracticeExamBySlug;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private TurnstileClient turnstileClient;

    private final List<Integer> userIds = new ArrayList<>();
    private final List<String> domainNames = new ArrayList<>();

    private Integer teacherId;
    private Integer otherTeacherId;
    private String concepts;
    private String security;

    @BeforeEach
    void setUp() {
        teacherId = createUser(RolesEnum.IS_TEACHER);
        otherTeacherId = createUser(RolesEnum.IS_TEACHER);
        concepts = domainName("Authoring Concepts");
        security = domainName("Authoring Security");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        for (Integer id : userIds) {
            jdbcTemplate.update("""
                    DELETE FROM alternatives WHERE question_id IN
                        (SELECT q.id FROM questions q JOIN practice_exams pe ON pe.id = q.practice_exam_id
                         WHERE pe.teacher_id = ?)
                    """, id);
            jdbcTemplate.update("""
                    DELETE FROM questions WHERE practice_exam_id IN (SELECT id FROM practice_exams WHERE teacher_id = ?)
                    """, id);
            jdbcTemplate.update("""
                    DELETE FROM practice_exam_domains WHERE practice_exam_id IN
                        (SELECT id FROM practice_exams WHERE teacher_id = ?)
                    """, id);
            jdbcTemplate.update("DELETE FROM practice_exams WHERE teacher_id = ?", id);
            jdbcTemplate.update("DELETE FROM exams WHERE teacher_id = ?", id);
            jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", id);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
        }
        domainNames.forEach(name -> jdbcTemplate.update("DELETE FROM domains WHERE LOWER(name) = LOWER(?)", name));
    }

    @Test
    @DisplayName("Should author an exam step by step and keep it hidden until published")
    void shouldAuthorStepByStep() {
        authenticateAs(teacherId);

        Integer examId = createExam.execute(new CreateExamRequest("Authoring Exam", "About it", "Cloud", 2, true)).getId();
        assertEquals(List.of(examId), listExams.execute().stream().map(ListInstructorExamsResponse::id).toList());

        CreateInstructorPracticeExamResponse created = createPracticeExam.execute(practiceExamRequest(examId, 3));
        assertThrows(NotFoundException.class, () -> getPracticeExamBySlug.execute(created.slug()));
        assertEquals(1, listExams.execute().getFirst().practiceExamCount());

        addDomains.execute(created.id(), domains(concepts, security));
        GetInstructorPracticeExamResponse detail = getPracticeExam.execute(created.id());
        Integer conceptsId = domainId(detail, concepts);
        Integer securityId = domainId(detail, security);

        CreateInstructorQuestionsResponse saved = createQuestions.execute(created.id(), new CreateInstructorQuestionsRequest("en", List.of(
                question(QuestionTypeEnum.OBJECTIVE, conceptsId, correct("Right"), wrong("Wrong"), wrong("Also wrong")),
                question(QuestionTypeEnum.MULTIPLE_CHOICE, securityId, correct("One"), correct("Two"), wrong("Three")),
                question(QuestionTypeEnum.TRUE_FALSE, conceptsId, correct("True"), wrong("False")))));
        assertEquals(3, saved.created());

        detail = getPracticeExam.execute(created.id());
        assertFalse(detail.isActive());
        assertEquals(3, detail.questionCount());
        assertEquals(3L, detail.questionCountByLanguage().get("en"));
        assertEquals(2, detail.domains().stream().filter(domain -> domain.id().equals(conceptsId)).findFirst().orElseThrow().questionCount());

        Page<ListInstructorQuestionsResponse> page = listQuestions.execute(created.id(), 0, 20);
        assertEquals(3, page.getTotalElements());
        ListInstructorQuestionsResponse multiple = page.getContent().stream()
                .filter(item -> item.type().equals("MULTIPLE_CHOICE")).findFirst().orElseThrow();
        assertEquals(3, multiple.alternativeCount());
        assertEquals(2, multiple.correctCount());
        assertEquals(security, multiple.domain().name());

        updateStatus.execute(created.id(), new UpdatePracticeExamStatusRequest(true));
        assertEquals(created.id(), getPracticeExamBySlug.execute(created.slug()).getId());
        assertTrue(listPracticeExams.execute().getFirst().isActive());
    }

    @Test
    @DisplayName("Should reject the whole batch when one question is invalid")
    void shouldRejectInvalidBatch() {
        authenticateAs(teacherId);
        Integer practiceExamId = practiceExamWithDomains();
        Integer conceptsId = domainId(getPracticeExam.execute(practiceExamId), concepts);
        Integer unlinkedId = jdbcTemplate.queryForObject(
                "INSERT INTO domains (name) VALUES (?) RETURNING id", Integer.class, domainName("Authoring Unlinked"));

        assertThrows(UnprocessableEntityException.class, () -> createQuestions.execute(practiceExamId, batch(
                question(QuestionTypeEnum.OBJECTIVE, conceptsId, correct("Right"), wrong("Wrong")),
                question(QuestionTypeEnum.OBJECTIVE, unlinkedId, correct("Right"), wrong("Wrong")))));
        assertThrows(UnprocessableEntityException.class, () -> createQuestions.execute(practiceExamId, batch(
                question(QuestionTypeEnum.OBJECTIVE, conceptsId, correct("Right"), correct("Also right"), wrong("Wrong")))));
        assertThrows(UnprocessableEntityException.class, () -> createQuestions.execute(practiceExamId, batch(
                question(QuestionTypeEnum.MULTIPLE_CHOICE, conceptsId, correct("Right"), correct("Also right")))));
        assertThrows(UnprocessableEntityException.class, () -> createQuestions.execute(practiceExamId, batch(
                question(QuestionTypeEnum.TRUE_FALSE, conceptsId, correct("True"), wrong("False"), wrong("Maybe")))));
        assertThrows(UnprocessableEntityException.class, () -> createQuestions.execute(practiceExamId, batch(
                question(QuestionTypeEnum.DESCRIPTIVE, conceptsId, correct("Right"), wrong("Wrong")))));

        assertEquals(0, getPracticeExam.execute(practiceExamId).questionCount());
    }

    @Test
    @DisplayName("Should refuse duplicate domains and removing a domain that still has questions")
    void shouldGuardDomains() {
        authenticateAs(teacherId);
        Integer practiceExamId = practiceExamWithDomains();
        Integer conceptsId = domainId(getPracticeExam.execute(practiceExamId), concepts);
        Integer securityId = domainId(getPracticeExam.execute(practiceExamId), security);

        assertThrows(UnprocessableEntityException.class, () -> addDomains.execute(practiceExamId, domains(concepts.toUpperCase())));
        String extra = domainName("Authoring Extra");
        assertThrows(UnprocessableEntityException.class, () -> addDomains.execute(practiceExamId, domains(extra, extra)));

        removeDomain.execute(practiceExamId, securityId);
        assertEquals(1, getPracticeExam.execute(practiceExamId).domains().size());

        Integer questionId = createQuestions.execute(practiceExamId, batch(
                question(QuestionTypeEnum.OBJECTIVE, conceptsId, correct("Right"), wrong("Wrong")))).questionIds().getFirst();
        assertThrows(UnprocessableEntityException.class, () -> removeDomain.execute(practiceExamId, conceptsId));

        deleteQuestion.execute(practiceExamId, questionId);
        removeDomain.execute(practiceExamId, conceptsId);
        assertTrue(getPracticeExam.execute(practiceExamId).domains().isEmpty());
    }

    @Test
    @DisplayName("Should publish only once one language has enough questions for an attempt")
    void shouldRequireEnoughQuestionsToPublish() {
        authenticateAs(teacherId);
        Integer practiceExamId = practiceExamWithDomains();
        Integer conceptsId = domainId(getPracticeExam.execute(practiceExamId), concepts);
        UpdatePracticeExamStatusRequest publish = new UpdatePracticeExamStatusRequest(true);

        assertThrows(UnprocessableEntityException.class, () -> updateStatus.execute(practiceExamId, publish));

        createQuestions.execute(practiceExamId, new CreateInstructorQuestionsRequest("en", List.of(
                question(QuestionTypeEnum.OBJECTIVE, conceptsId, correct("Right"), wrong("Wrong")))));
        createQuestions.execute(practiceExamId, new CreateInstructorQuestionsRequest("pt", List.of(
                question(QuestionTypeEnum.OBJECTIVE, conceptsId, correct("Certo"), wrong("Errado")))));
        assertThrows(UnprocessableEntityException.class, () -> updateStatus.execute(practiceExamId, publish));

        createQuestions.execute(practiceExamId, batch(
                question(QuestionTypeEnum.OBJECTIVE, conceptsId, correct("Right"), wrong("Wrong"))));
        updateStatus.execute(practiceExamId, publish);
        assertTrue(getPracticeExam.execute(practiceExamId).isActive());
    }

    @Test
    @DisplayName("Should hide another instructor's exams and practice exams")
    void shouldScopeToOwner() {
        authenticateAs(teacherId);
        Integer examId = createExam.execute(new CreateExamRequest("Owned Exam", null, null, 1, true)).getId();
        Integer practiceExamId = practiceExamWithDomains();

        authenticateAs(otherTeacherId);
        assertTrue(listExams.execute().isEmpty());
        assertTrue(listPracticeExams.execute().isEmpty());
        assertThrows(NotFoundException.class, () -> getPracticeExam.execute(practiceExamId));
        assertThrows(NotFoundException.class, () -> createPracticeExam.execute(practiceExamRequest(examId, 3)));
        assertThrows(NotFoundException.class, () -> addDomains.execute(practiceExamId, domains(domainName("Authoring Intruder"))));
        assertThrows(NotFoundException.class,
                () -> updateStatus.execute(practiceExamId, new UpdatePracticeExamStatusRequest(false)));
    }

    @Test
    @DisplayName("Should keep authoring away from students")
    void shouldRequireTeacherRole() {
        authenticateAs(createUser(RolesEnum.IS_STUDENT));

        assertThrows(AccessDeniedException.class, () -> listExams.execute());
        assertThrows(AccessDeniedException.class, () -> listPracticeExams.execute());
    }

    private Integer practiceExamWithDomains() {
        Integer examId = createExam.execute(new CreateExamRequest("Domain Exam", null, null, 1, true)).getId();
        Integer practiceExamId = createPracticeExam.execute(practiceExamRequest(examId, 2)).id();
        addDomains.execute(practiceExamId, domains(concepts, security));
        return practiceExamId;
    }

    private CreateInstructorPracticeExamRequest practiceExamRequest(Integer examId, int numberOfQuestions) {
        return new CreateInstructorPracticeExamRequest(
                examId, "Authoring Practice " + System.nanoTime(), "A practice exam", 70, 90, numberOfQuestions);
    }

    private AddPracticeExamDomainsRequest domains(String... names) {
        return new AddPracticeExamDomainsRequest(List.of(names).stream()
                .map(name -> new AddPracticeExamDomainsRequest.DomainRequest(name, 20.0, 30.0))
                .toList());
    }

    private CreateInstructorQuestionsRequest batch(CreateInstructorQuestionsRequest.QuestionRequest... questions) {
        return new CreateInstructorQuestionsRequest("en", List.of(questions));
    }

    private CreateInstructorQuestionsRequest.QuestionRequest question(
            QuestionTypeEnum type, Integer domainId, CreateInstructorQuestionsRequest.AlternativeRequest... alternatives) {
        return new CreateInstructorQuestionsRequest.QuestionRequest(
                type, domainId, "Which one? " + System.nanoTime(), "Because.", List.of(alternatives));
    }

    private CreateInstructorQuestionsRequest.AlternativeRequest correct(String content) {
        return new CreateInstructorQuestionsRequest.AlternativeRequest(content, true);
    }

    private CreateInstructorQuestionsRequest.AlternativeRequest wrong(String content) {
        return new CreateInstructorQuestionsRequest.AlternativeRequest(content, false);
    }

    private Integer domainId(GetInstructorPracticeExamResponse detail, String name) {
        return detail.domains().stream()
                .filter(domain -> domain.name().equals(name))
                .findFirst()
                .orElseThrow()
                .id();
    }

    private String domainName(String prefix) {
        String name = prefix + " " + System.nanoTime();
        domainNames.add(name);
        return name;
    }

    private Integer createUser(RolesEnum roleName) {
        Role role = roleRepository.findByName(roleName.name()).orElseThrow();
        User user = new User();
        user.setName("Authoring User");
        user.setEmail("authoring-%s@example.com".formatted(System.nanoTime()));
        user.setPassword("irrelevant");
        user.setRoles(new HashSet<>(Set.of(role)));
        Integer id = userRepository.save(user).getId();
        userIds.add(id);
        return id;
    }

    private void authenticateAs(Integer userId) {
        AuthenticatedUser principal = new AuthenticatedUser(userRepository.findById(userId).orElseThrow());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}

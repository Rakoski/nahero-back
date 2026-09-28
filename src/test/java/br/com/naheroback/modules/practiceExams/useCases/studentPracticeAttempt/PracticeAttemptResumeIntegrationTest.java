package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt;

import br.com.naheroback.common.exceptions.custom.ConflictException;
import br.com.naheroback.modules.auth.entities.AuthenticatedUser;
import br.com.naheroback.modules.practiceExams.entities.enums.PracticeAttemptStatusesEnum;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.create.CreateStudentPracticeAttemptRequest;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.create.CreateStudentPracticeAttemptUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.finish.FinishStudentPracticeAttemptRequest;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.finish.FinishStudentPracticeAttemptUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getInProgress.GetInProgressAttemptUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getState.GetAttemptStateResponse;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getState.GetAttemptStateUseCase;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the resume lifecycle end to end: practice exams are unlimited and free, auto-saved
 * answers survive leaving the page, and switching practice exams needs the student's
 * confirmation.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "reengagement.enabled=false",
        "payment.reconcile.enabled=false"
})
class PracticeAttemptResumeIntegrationTest {

    private static final int QUESTIONS_PER_EXAM = 2;

    @Autowired
    private CreateStudentPracticeAttemptUseCase createAttempt;

    @Autowired
    private SaveAttemptProgressUseCase saveProgress;

    @Autowired
    private GetAttemptStateUseCase getState;

    @Autowired
    private GetInProgressAttemptUseCase getInProgress;

    @Autowired
    private FinishStudentPracticeAttemptUseCase finishAttempt;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Integer studentId;
    private Integer examId;
    private Integer firstPracticeExamId;
    private Integer secondPracticeExamId;
    private List<Integer> firstExamQuestionIds;
    private List<Integer> correctAlternativeIds;

    @BeforeEach
    void setUp() {
        Role student = roleRepository.findByName(RolesEnum.IS_STUDENT.name()).orElseThrow();

        User user = new User();
        user.setName("Resuming Student");
        user.setEmail("resume-%s@example.com".formatted(System.nanoTime()));
        user.setPassword("irrelevant");
        user.setRoles(new HashSet<>(Set.of(student)));

        User saved = userRepository.save(user);
        studentId = saved.getId();

        examId = jdbcTemplate.queryForObject("""
                INSERT INTO exams (title, difficulty_level, is_active) VALUES ('Resume Exam', 2, true) RETURNING id
                """, Integer.class);

        firstPracticeExamId = insertPracticeExam("Resume Practice A");
        secondPracticeExamId = insertPracticeExam("Resume Practice B");

        firstExamQuestionIds = insertQuestions(firstPracticeExamId);
        correctAlternativeIds = firstExamQuestionIds.stream().map(this::correctAlternativeOf).toList();
        insertQuestions(secondPracticeExamId);

        authenticateAs(saved);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();

        jdbcTemplate.update("""
                DELETE FROM student_attempt_answer_drafts WHERE student_practice_attempt_id IN
                    (SELECT id FROM student_practice_attempts WHERE practice_exam_id IN (?, ?))
                """, firstPracticeExamId, secondPracticeExamId);
        jdbcTemplate.update("""
                DELETE FROM student_answers WHERE student_practice_attempt_id IN
                    (SELECT id FROM student_practice_attempts WHERE practice_exam_id IN (?, ?))
                """, firstPracticeExamId, secondPracticeExamId);
        jdbcTemplate.update("DELETE FROM student_practice_attempts WHERE practice_exam_id IN (?, ?)",
                firstPracticeExamId, secondPracticeExamId);
        jdbcTemplate.update("DELETE FROM enrollments WHERE student_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM alternatives WHERE question_id IN (SELECT id FROM questions WHERE practice_exam_id IN (?, ?))",
                firstPracticeExamId, secondPracticeExamId);
        jdbcTemplate.update("DELETE FROM questions WHERE practice_exam_id IN (?, ?)",
                firstPracticeExamId, secondPracticeExamId);
        jdbcTemplate.update("DELETE FROM practice_exams WHERE id IN (?, ?)", firstPracticeExamId, secondPracticeExamId);
        jdbcTemplate.update("DELETE FROM exams WHERE id = ?", examId);
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", studentId);
    }

    @Test
    @DisplayName("Should hand back the running attempt when the same practice exam is started again")
    void shouldResumeInsteadOfStartingOver() {
        Integer first = createAttempt.execute(
                new CreateStudentPracticeAttemptRequest(firstPracticeExamId, null), Locale.ENGLISH);
        Integer second = createAttempt.execute(
                new CreateStudentPracticeAttemptRequest(firstPracticeExamId, null), Locale.ENGLISH);

        assertEquals(first, second);
        assertEquals(1, attemptCount());
    }

    @Test
    @DisplayName("Should give back the auto-saved answers and the question the student stopped on")
    void shouldRestoreAutoSavedProgress() {
        Integer attemptId = createAttempt.execute(
                new CreateStudentPracticeAttemptRequest(firstPracticeExamId, null), Locale.ENGLISH);

        Integer answeredQuestionId = firstExamQuestionIds.getFirst();
        Integer chosenAlternativeId = correctAlternativeOf(answeredQuestionId);

        saveProgress.execute(attemptId,
                new SaveAttemptProgressRequest(answeredQuestionId, List.of(chosenAlternativeId), 1));

        GetAttemptStateResponse state = getState.execute(attemptId);

        assertEquals(1, state.lastQuestionIndex());
        assertEquals(1, state.answeredCount());
        assertEquals(QUESTIONS_PER_EXAM, state.totalQuestions());
        assertEquals(List.of(chosenAlternativeId), state.answers().getFirst().alternativeIds());
        assertTrue(state.remainingSeconds() > 0);

        Optional<GetAttemptStateResponse> inProgress = getInProgress.execute();

        assertTrue(inProgress.isPresent());
        assertEquals(attemptId, inProgress.get().attemptId());
    }

    @Test
    @DisplayName("Should replace a previously auto-saved answer instead of stacking a second one")
    void shouldOverwriteAnAutoSavedAnswer() {
        Integer attemptId = createAttempt.execute(
                new CreateStudentPracticeAttemptRequest(firstPracticeExamId, null), Locale.ENGLISH);

        Integer questionId = firstExamQuestionIds.getFirst();
        Integer correct = correctAlternativeOf(questionId);
        Integer wrong = wrongAlternativeOf(questionId);

        saveProgress.execute(attemptId, new SaveAttemptProgressRequest(questionId, List.of(wrong), 0));
        saveProgress.execute(attemptId, new SaveAttemptProgressRequest(questionId, List.of(correct), 0));

        GetAttemptStateResponse state = getState.execute(attemptId);

        assertEquals(1, state.answers().size());
        assertEquals(List.of(correct), state.answers().getFirst().alternativeIds());
    }

    @Test
    @DisplayName("Should refuse to start another practice exam until the switch is confirmed")
    void shouldRefuseAnUnconfirmedSwitch() {
        Integer firstAttemptId = createAttempt.execute(
                new CreateStudentPracticeAttemptRequest(firstPracticeExamId, null), Locale.ENGLISH);

        assertThrows(ConflictException.class, () -> createAttempt.execute(
                new CreateStudentPracticeAttemptRequest(secondPracticeExamId, null), Locale.ENGLISH));

        assertEquals(PracticeAttemptStatusesEnum.IN_PROGRESS.getId(), statusOf(firstAttemptId));
    }

    @Test
    @DisplayName("Should let the student switch practice exams once the switch is confirmed")
    void shouldSwitchExamsOnConfirmation() {
        Integer firstAttemptId = createAttempt.execute(
                new CreateStudentPracticeAttemptRequest(firstPracticeExamId, null), Locale.ENGLISH);

        Integer secondAttemptId = createAttempt.execute(
                new CreateStudentPracticeAttemptRequest(secondPracticeExamId, true), Locale.ENGLISH);

        assertNotEquals(firstAttemptId, secondAttemptId);
        assertEquals(PracticeAttemptStatusesEnum.ABANDONED.getId(), statusOf(firstAttemptId));
        assertEquals(PracticeAttemptStatusesEnum.IN_PROGRESS.getId(), statusOf(secondAttemptId));
    }

    @Test
    @DisplayName("Should score the auto-saved answers on finish")
    void shouldScoreAutoSavedAnswersOnFinish() {
        Integer attemptId = createAttempt.execute(
                new CreateStudentPracticeAttemptRequest(firstPracticeExamId, null), Locale.ENGLISH);

        for (Integer questionId : firstExamQuestionIds) {
            saveProgress.execute(attemptId,
                    new SaveAttemptProgressRequest(questionId, List.of(correctAlternativeOf(questionId)), 0));
        }

        finishAttempt.execute(new FinishStudentPracticeAttemptRequest(attemptId, null));

        assertEquals(PracticeAttemptStatusesEnum.COMPLETED.getId(), statusOf(attemptId));
        assertEquals(QUESTIONS_PER_EXAM, scoreOf(attemptId));
        assertTrue(getInProgress.execute().isEmpty());
    }

    @Test
    @DisplayName("Should keep letting the student start practice exams after finishing one")
    void shouldKeepPracticeExamsFreeAfterAFinishedAttempt() {
        Integer attemptId = createAttempt.execute(
                new CreateStudentPracticeAttemptRequest(firstPracticeExamId, null), Locale.ENGLISH);

        saveProgress.execute(attemptId, new SaveAttemptProgressRequest(
                firstExamQuestionIds.getFirst(), correctAlternativeIds.subList(0, 1), 0));

        finishAttempt.execute(new FinishStudentPracticeAttemptRequest(attemptId, null));

        Integer nextAttemptId = assertDoesNotThrow(() -> createAttempt.execute(
                new CreateStudentPracticeAttemptRequest(secondPracticeExamId, null), Locale.ENGLISH));

        assertEquals(PracticeAttemptStatusesEnum.IN_PROGRESS.getId(), statusOf(nextAttemptId));
    }

    @Test
    @DisplayName("Should reject auto-save on an attempt that is already finished")
    void shouldRejectAutoSaveAfterFinish() {
        Integer attemptId = createAttempt.execute(
                new CreateStudentPracticeAttemptRequest(firstPracticeExamId, null), Locale.ENGLISH);

        finishAttempt.execute(new FinishStudentPracticeAttemptRequest(attemptId, null));

        assertThrows(ConflictException.class, () -> saveProgress.execute(attemptId,
                new SaveAttemptProgressRequest(firstExamQuestionIds.getFirst(), correctAlternativeIds.subList(0, 1), 0)));
    }

    @Test
    @DisplayName("Should resume only the newest attempt and abandon every other one still open")
    void shouldCollapseMultipleInProgressAttemptsToOne() {
        Integer newest = createAttempt.execute(
                new CreateStudentPracticeAttemptRequest(firstPracticeExamId, null), Locale.ENGLISH);

        Integer enrollmentId = enrollmentOf(newest);
        Integer olderOne = insertInProgressAttempt(enrollmentId, LocalDateTime.now().minusMinutes(20));
        Integer olderTwo = insertInProgressAttempt(enrollmentId, LocalDateTime.now().minusMinutes(10));

        Optional<GetAttemptStateResponse> resumed = getInProgress.execute();

        assertTrue(resumed.isPresent());
        assertEquals(newest, resumed.get().attemptId());
        assertEquals(PracticeAttemptStatusesEnum.IN_PROGRESS.getId(), statusOf(newest));
        assertEquals(PracticeAttemptStatusesEnum.ABANDONED.getId(), statusOf(olderOne));
        assertEquals(PracticeAttemptStatusesEnum.ABANDONED.getId(), statusOf(olderTwo));
    }

    @Test
    @DisplayName("Should not offer an attempt whose time limit already expired")
    void shouldNotResumeAnAttemptWhoseTimeHasRunOut() {
        Integer attemptId = createAttempt.execute(
                new CreateStudentPracticeAttemptRequest(firstPracticeExamId, null), Locale.ENGLISH);

        jdbcTemplate.update("UPDATE student_practice_attempts SET start_time = ? WHERE id = ?",
                LocalDateTime.now().minusHours(3), attemptId);

        assertTrue(getInProgress.execute().isEmpty());
        assertEquals(PracticeAttemptStatusesEnum.ABANDONED.getId(), statusOf(attemptId));
    }

    private Integer insertPracticeExam(String title) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO practice_exams (exam_id, title, slug, passing_score, time_limit, number_of_questions, is_active)
                VALUES (?, ?, ?, 50, 60, ?, true) RETURNING id
                """, Integer.class, examId, title, title.toLowerCase().replace(' ', '-') + "-" + System.nanoTime(),
                QUESTIONS_PER_EXAM);
    }

    private List<Integer> insertQuestions(Integer practiceExamId) {
        return java.util.stream.IntStream.range(0, QUESTIONS_PER_EXAM)
                .mapToObj(index -> {
                    Integer questionId = jdbcTemplate.queryForObject("""
                            INSERT INTO questions (practice_exam_id, question_type_id, content, points, version, is_active, language)
                            VALUES (?, 3, ?, 1, 1, true, 'en') RETURNING id
                            """, Integer.class, practiceExamId, "Question %d".formatted(index));

                    jdbcTemplate.update("""
                            INSERT INTO alternatives (question_id, content, is_correct, version, is_active)
                            VALUES (?, 'Correct', true, 1, true), (?, 'Wrong', false, 1, true)
                            """, questionId, questionId);

                    return questionId;
                })
                .toList();
    }

    private Integer correctAlternativeOf(Integer questionId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM alternatives WHERE question_id = ? AND is_correct = true", Integer.class, questionId);
    }

    private Integer wrongAlternativeOf(Integer questionId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM alternatives WHERE question_id = ? AND is_correct = false", Integer.class, questionId);
    }

    private int statusOf(Integer attemptId) {
        return jdbcTemplate.queryForObject("SELECT status FROM student_practice_attempts WHERE id = ?",
                Integer.class, attemptId);
    }

    private int scoreOf(Integer attemptId) {
        return jdbcTemplate.queryForObject("SELECT score FROM student_practice_attempts WHERE id = ?",
                Integer.class, attemptId);
    }

    private int attemptCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM student_practice_attempts WHERE practice_exam_id = ?",
                Integer.class, firstPracticeExamId);
    }

    private Integer enrollmentOf(Integer attemptId) {
        return jdbcTemplate.queryForObject(
                "SELECT enrollment_id FROM student_practice_attempts WHERE id = ?", Integer.class, attemptId);
    }

    private Integer insertInProgressAttempt(Integer enrollmentId, LocalDateTime startTime) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO student_practice_attempts (enrollment_id, practice_exam_id, status, start_time, language)
                VALUES (?, ?, 1, ?, 'en') RETURNING id
                """, Integer.class, enrollmentId, firstPracticeExamId, startTime);
    }

    private void authenticateAs(User user) {
        AuthenticatedUser principal = new AuthenticatedUser(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}

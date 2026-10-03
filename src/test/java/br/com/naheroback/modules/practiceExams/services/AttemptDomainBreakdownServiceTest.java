package br.com.naheroback.modules.practiceExams.services;

import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.entities.StudentAnswer;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentAnswerRepository;
import br.com.naheroback.modules.practiceExams.services.AttemptDomainBreakdownService.DomainScore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttemptDomainBreakdownServiceTest {

    @Mock
    private StudentAnswerRepository studentAnswerRepository;

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private AttemptDomainBreakdownService service;

    @Test
    @DisplayName("Should count correct and total answers per domain from the attempt's answers")
    void shouldCountPerDomain() {
        StudentPracticeAttempt attempt = attempt(10);
        when(studentAnswerRepository.findAllByStudentPracticeAttemptId(10)).thenReturn(List.of(
                answer(1, true), answer(2, false), answer(3, true), answer(4, true)));
        when(questionRepository.findAllByIdIn(anyList())).thenReturn(List.of(
                question(1, "Security and Compliance"),
                question(2, "Security and Compliance"),
                question(3, "Cloud Concepts"),
                question(4, "Cloud Concepts")));

        List<DomainScore> breakdown = service.breakdown(attempt);

        assertEquals(List.of(
                new DomainScore("Security and Compliance", 1, 2),
                new DomainScore("Cloud Concepts", 2, 2)), breakdown);
    }

    @Test
    @DisplayName("Should sort by ratio ascending, then by total descending")
    void shouldSortWeakestFirst() {
        List<StudentAnswer> answers = List.of(
                answer(1, true), answer(2, true), answer(3, true),
                answer(4, true), answer(5, false),
                answer(6, false), answer(7, true), answer(8, false), answer(9, true),
                answer(10, false), answer(11, true));
        Map<Integer, String> domains = Map.ofEntries(
                Map.entry(1, "Perfect"), Map.entry(2, "Perfect"), Map.entry(3, "Perfect"),
                Map.entry(4, "Half small"), Map.entry(5, "Half small"),
                Map.entry(6, "Half big"), Map.entry(7, "Half big"), Map.entry(8, "Half big"), Map.entry(9, "Half big"),
                Map.entry(10, "Weak"), Map.entry(11, "Strong"));

        List<DomainScore> breakdown = AttemptDomainBreakdownService.breakdown(answers, domains);

        assertEquals(List.of("Weak", "Half big", "Half small", "Perfect", "Strong"),
                breakdown.stream().map(DomainScore::domain).toList());
        assertEquals("Weak", AttemptDomainBreakdownService.weakest(breakdown).orElseThrow().domain());
    }

    @Test
    @DisplayName("Should omit questions that have no domain")
    void shouldOmitNullDomains() {
        StudentPracticeAttempt attempt = attempt(11);
        when(studentAnswerRepository.findAllByStudentPracticeAttemptId(11)).thenReturn(List.of(
                answer(1, false), answer(2, true)));
        when(questionRepository.findAllByIdIn(anyList())).thenReturn(List.of(
                question(1, null),
                question(2, "Cloud Concepts")));

        List<DomainScore> breakdown = service.breakdown(attempt);

        assertEquals(List.of(new DomainScore("Cloud Concepts", 1, 1)), breakdown);
    }

    @Test
    @DisplayName("Should count a question once even when it has one answer row per selected alternative")
    void shouldCountMultiRowAnswersOnce() {
        List<DomainScore> breakdown = AttemptDomainBreakdownService.breakdown(
                List.of(answer(1, true), answer(1, true), answer(2, false), answer(2, false)),
                Map.of(1, "Cloud Concepts", 2, "Cloud Concepts"));

        assertEquals(List.of(new DomainScore("Cloud Concepts", 1, 2)), breakdown);
    }

    @Test
    @DisplayName("Should return an empty breakdown without querying questions when there are no answers")
    void shouldReturnEmptyWithoutAnswers() {
        StudentPracticeAttempt attempt = attempt(12);
        when(studentAnswerRepository.findAllByStudentPracticeAttemptId(12)).thenReturn(List.of());

        List<DomainScore> breakdown = service.breakdown(attempt);

        assertTrue(breakdown.isEmpty());
        assertTrue(AttemptDomainBreakdownService.weakest(breakdown).isEmpty());
        verifyNoInteractions(questionRepository);
    }

    private StudentPracticeAttempt attempt(int id) {
        StudentPracticeAttempt attempt = new StudentPracticeAttempt();
        attempt.setId(id);
        return attempt;
    }

    private StudentAnswer answer(int questionId, boolean correct) {
        StudentAnswer answer = new StudentAnswer();
        answer.setQuestionId(questionId);
        answer.setQuestionVersion(1);
        answer.setIsCorrect(correct);
        return answer;
    }

    private Question question(int id, String domain) {
        Question question = new Question();
        question.setId(id);
        question.setDomain(domain);
        return question;
    }
}

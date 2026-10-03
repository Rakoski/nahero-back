package br.com.naheroback.modules.practiceExams.services;

import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.entities.StudentAnswer;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttemptDomainBreakdownService {
    private final StudentAnswerRepository studentAnswerRepository;
    private final QuestionRepository questionRepository;

    public record DomainScore(String domain, int correct, int total) {}

    private static final Comparator<DomainScore> WEAKEST_FIRST = ((Comparator<DomainScore>) (a, b) ->
            Long.compare((long) a.correct() * b.total(), (long) b.correct() * a.total()))
            .thenComparing(DomainScore::total, Comparator.reverseOrder())
            .thenComparing(DomainScore::domain);

    public List<DomainScore> breakdown(StudentPracticeAttempt attempt) {
        List<StudentAnswer> answers = studentAnswerRepository.findAllByStudentPracticeAttemptId(attempt.getId());
        return breakdown(answers, domainsOf(answers));
    }

    public Map<Integer, String> domainsOf(List<StudentAnswer> answers) {
        List<Integer> questionIds = answers.stream()
                .map(StudentAnswer::getQuestionId)
                .distinct()
                .toList();

        if (questionIds.isEmpty()) return Map.of();

        return questionRepository.findAllByIdIn(questionIds).stream()
                .filter(question -> question.getDomain() != null && !question.getDomain().isBlank())
                .collect(Collectors.toMap(Question::getId, Question::getDomain));
    }

    public static List<DomainScore> breakdown(List<StudentAnswer> answers, Map<Integer, String> domainByQuestionId) {
        Map<Integer, Boolean> correctByQuestionId = answers.stream()
                .collect(Collectors.toMap(
                        StudentAnswer::getQuestionId,
                        answer -> Boolean.TRUE.equals(answer.getIsCorrect()),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));

        Map<String, int[]> tally = new LinkedHashMap<>();
        correctByQuestionId.forEach((questionId, correct) -> {
            String domain = domainByQuestionId.get(questionId);
            if (domain == null) return;
            int[] counts = tally.computeIfAbsent(domain, key -> new int[2]);
            if (correct) counts[0]++;
            counts[1]++;
        });

        return tally.entrySet().stream()
                .map(entry -> new DomainScore(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .sorted(WEAKEST_FIRST)
                .toList();
    }

    public static Optional<DomainScore> weakest(List<DomainScore> breakdown) {
        return breakdown.stream().findFirst();
    }
}

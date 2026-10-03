package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getResult;

import br.com.naheroback.common.utils.Constants;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.entities.StudentAnswer;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.services.AttemptDomainBreakdownService;
import br.com.naheroback.modules.practiceExams.services.AttemptDomainBreakdownService.DomainScore;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class GetResultResponse {
    private Boolean passed;
    private Integer score;
    private Integer answers;
    private Integer correctAnswers;
    private Integer incorrectAnswers;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer timeLimit;
    private Integer timeSpentInMinutes;
    private Integer passingPercentageScore;
    private String attemptStatus;
    private Integer numberOfQuestions;
    private List<Integer> incorrectQuestionIds;
    private List<QuestionResult> questions;
    private List<DomainScore> domains;
    private String weakestDomain;
    private String practiceExamSlug;
    private String practiceExamTitle;

    public record QuestionResult(Integer questionId, String domain, Boolean correct) {}

    public static GetResultResponse toPresentation(StudentPracticeAttempt attempt,
                                                   List<StudentAnswer> studentAnswers,
                                                   Map<Integer, String> domainByQuestionId) {
        GetResultResponse response = new GetResultResponse();
        PracticeExam attemptedPracticeExam = attempt.getPracticeExam();

        response.setPassed(attempt.getPassed());
        response.setScore(attempt.getScore());

        AnswerAnalysis analysis = analyzeAnswers(studentAnswers);
        response.setIncorrectQuestionIds(analysis.incorrectQuestionIds());
        response.setCorrectAnswers(analysis.correctCount());
        response.setIncorrectAnswers(analysis.incorrectCount());
        response.setAnswers(analysis.totalQuestions());
        response.setQuestions(toQuestionResults(analysis.resultsByQuestionId(), domainByQuestionId));

        List<DomainScore> domains = AttemptDomainBreakdownService.breakdown(studentAnswers, domainByQuestionId);
        response.setDomains(domains);
        response.setWeakestDomain(AttemptDomainBreakdownService.weakest(domains).map(DomainScore::domain).orElse(null));

        response.setStartTime(attempt.getStartTime());
        response.setEndTime(attempt.getEndTime());
        response.setTimeSpentInMinutes(calculateTimeSpent(attempt.getStartTime(), attempt.getEndTime()));

        response.setNumberOfQuestions(getNumberOfQuestions(attemptedPracticeExam));
        response.setTimeLimit(attemptedPracticeExam.getTimeLimit());
        response.setPassingPercentageScore(attemptedPracticeExam.getPassingScore());
        response.setPracticeExamSlug(attemptedPracticeExam.getSlug());
        response.setPracticeExamTitle(attemptedPracticeExam.getTitle());

        response.setAttemptStatus(attempt.getAttemptStatus().getName());

        return response;
    }

    private static AnswerAnalysis analyzeAnswers(List<StudentAnswer> answers) {
        Map<Integer, Boolean> questionResults = answers.stream()
                .collect(Collectors.toMap(
                        StudentAnswer::getQuestionId,
                        StudentAnswer::getIsCorrect,
                        (existing, replacement) -> existing
                ));

        List<Integer> incorrectIds = questionResults.entrySet().stream()
                .filter(entry -> Boolean.FALSE.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        int correctCount = (int) questionResults.values().stream()
                .filter(Boolean.TRUE::equals)
                .count();

        return new AnswerAnalysis(
                questionResults.size(),
                correctCount,
                incorrectIds.size(),
                incorrectIds,
                questionResults
        );
    }

    private static List<QuestionResult> toQuestionResults(Map<Integer, Boolean> resultsByQuestionId,
                                                          Map<Integer, String> domainByQuestionId) {
        return resultsByQuestionId.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new QuestionResult(
                        entry.getKey(),
                        domainByQuestionId.get(entry.getKey()),
                        entry.getValue()))
                .toList();
    }

    private static int calculateTimeSpent(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return (int) Duration.between(startTime, endTime).toMinutes();
    }

    private static int getNumberOfQuestions(PracticeExam practiceExam) {
        return practiceExam.getNumberOfQuestions() != null
                ? practiceExam.getNumberOfQuestions()
                : Constants.MAX_EXAM_QUESTIONS;
    }

    private record AnswerAnalysis(
            int totalQuestions,
            int correctCount,
            int incorrectCount,
            List<Integer> incorrectQuestionIds,
            Map<Integer, Boolean> resultsByQuestionId
    ) {}
}

package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.finish;

import br.com.naheroback.common.utils.Constants;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.entities.StudentAnswer;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class FinishStudentPracticeAttemptResponse {
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

    public static FinishStudentPracticeAttemptResponse toPresentation(StudentPracticeAttempt attempt, List<StudentAnswer> answers) {
        FinishStudentPracticeAttemptResponse response = new FinishStudentPracticeAttemptResponse();
        PracticeExam attemptedPracticeExam = attempt.getPracticeExam();

        response.setPassed(attempt.getPassed());
        response.setScore(attempt.getScore());

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

        response.setIncorrectQuestionIds(incorrectIds);
        response.setCorrectAnswers(correctCount);
        response.setIncorrectAnswers(incorrectIds.size());
        response.setAnswers(questionResults.size());

        response.setStartTime(attempt.getStartTime());
        response.setEndTime(attempt.getEndTime());

        int timeSpent = 0;
        if (attempt.getStartTime() != null && attempt.getEndTime() != null) {
            timeSpent = (int) Duration.between(attempt.getStartTime(), attempt.getEndTime()).toMinutes();
        }
        response.setTimeSpentInMinutes(timeSpent);

        response.setNumberOfQuestions(attemptedPracticeExam.getNumberOfQuestions() != null
                ? attemptedPracticeExam.getNumberOfQuestions()
                : Constants.MAX_EXAM_QUESTIONS);
        response.setTimeLimit(attemptedPracticeExam.getPassingScore());
        response.setPassingPercentageScore(attemptedPracticeExam.getPassingScore());

        response.setAttemptStatus(attempt.getAttemptStatus().getName());

        return response;
    }
}
package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.finish;

import br.com.naheroback.modules.practiceExams.entities.StudentAnswer;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FinishStudentPracticeAttemptResponse {
    private Boolean passed;
    private Integer score;
    private Integer totalQuestions;
    private Integer correctAnswers;
    private Integer incorrectAnswers;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long timeSpentInMinutes;
    private String attemptStatus;

    public static FinishStudentPracticeAttemptResponse toPresentation(StudentPracticeAttempt attempt, List<StudentAnswer> answers, int questions) {
        FinishStudentPracticeAttemptResponse response = new FinishStudentPracticeAttemptResponse();

        response.setPassed(attempt.getPassed());
        response.setScore(attempt.getScore());
        response.setTotalQuestions(questions);

        int correct = (int) answers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
        int incorrect = (int) answers.stream().filter(a -> Boolean.FALSE.equals(a.getIsCorrect())).count();

        response.setCorrectAnswers(correct);
        response.setIncorrectAnswers(incorrect);

        response.setStartTime(attempt.getStartTime());
        response.setEndTime(attempt.getEndTime());

        long timeSpent = 0;
        if (attempt.getStartTime() != null && attempt.getEndTime() != null) {
            timeSpent = Duration.between(attempt.getStartTime(), attempt.getEndTime()).toMinutes();
        }
        response.setTimeSpentInMinutes(timeSpent);

        response.setAttemptStatus(attempt.getAttemptStatus().getName());

        return response;
    }
}
package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.finish;

import br.com.naheroback.common.utils.Constants;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
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

    public static FinishStudentPracticeAttemptResponse toPresentation(StudentPracticeAttempt attempt, List<StudentAnswer> answers) {
        FinishStudentPracticeAttemptResponse response = new FinishStudentPracticeAttemptResponse();
        PracticeExam attemptedPracticeExam = attempt.getPracticeExam();

        response.setPassed(attempt.getPassed());
        response.setScore(attempt.getScore());
        response.setAnswers(answers.size());

        int correct = (int) answers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
        int incorrect = (int) answers.stream().filter(a -> Boolean.FALSE.equals(a.getIsCorrect())).count();

        response.setCorrectAnswers(correct);
        response.setIncorrectAnswers(incorrect);

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
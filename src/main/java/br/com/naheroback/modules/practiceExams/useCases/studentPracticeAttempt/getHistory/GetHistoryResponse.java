package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getHistory;

import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetHistoryResponse {
    private Integer attemptId;
    private Integer score;
    private Integer timeSpentInMinutes;
    private Integer timeLimit;
    private String practiceExamTitle;
    private Integer passingScore;

    public static GetHistoryResponse toPresentation(StudentPracticeAttempt attempt) {
        PracticeExam practiceExam = attempt.getPracticeExam();

        Integer timeSpent = null;
        if (attempt.getStartTime() != null && attempt.getEndTime() != null) {
            timeSpent = (int) Duration.between(attempt.getStartTime(), attempt.getEndTime()).toMinutes();
        }

        return GetHistoryResponse.builder()
                .attemptId(attempt.getId())
                .score(attempt.getScore())
                .timeSpentInMinutes(timeSpent)
                .timeLimit(practiceExam.getTimeLimit())
                .practiceExamTitle(practiceExam.getTitle())
                .passingScore(practiceExam.getPassingScore())
                .build();
    }
}


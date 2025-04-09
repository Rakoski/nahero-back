package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.finish;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record FinishStudentPracticeAttemptRequest(
        @NotNull(message = "studentPracticeAttemptId is required") Integer studentPracticeAttemptId,
        @NotEmpty(message = "answers are required") List<AnswerRequest> answers
) {
    public record AnswerRequest(
            @NotNull(message = "questionId is required") String questionId,
            List<String> alternativeIds,  // MULTIPLE_CHOICE, TRUE_FALSE, OBJECTIVE
            String descriptiveAnswer,     // DESCRIPTIVE
            Double sumAnswer              // SUM
    ) {}
}
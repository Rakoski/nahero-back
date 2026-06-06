package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.finish;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record FinishStudentPracticeAttemptRequest(
        @NotNull(message = "{studentpracticeattempt.id.required}") Integer studentPracticeAttemptId,
        @NotEmpty(message = "{studentpracticeattempt.answers.required}") List<AnswerRequest> answers
) {
    public record AnswerRequest(
            @NotNull(message = "{question.id.required}") String questionId,
            List<String> alternativeIds,  // MULTIPLE_CHOICE, TRUE_FALSE, OBJECTIVE
            String descriptiveAnswer,     // DESCRIPTIVE
            Double sumAnswer              // SUM
    ) {}
}
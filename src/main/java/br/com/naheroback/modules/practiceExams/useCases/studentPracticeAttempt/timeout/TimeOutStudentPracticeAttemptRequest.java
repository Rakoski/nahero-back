package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.timeout;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TimeOutStudentPracticeAttemptRequest(
        List<AnswerRequest> answers
) {
    public record AnswerRequest(
            @NotNull(message = "{question.id.required}") String questionId,
            List<String> alternativeIds,  // MULTIPLE_CHOICE, TRUE_FALSE, OBJECTIVE
            String descriptiveAnswer,     // DESCRIPTIVE
            Double sumAnswer              // SUM
    ) {}
}

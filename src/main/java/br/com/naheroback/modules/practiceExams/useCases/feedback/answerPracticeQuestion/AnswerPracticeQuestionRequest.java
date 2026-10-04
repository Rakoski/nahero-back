package br.com.naheroback.modules.practiceExams.useCases.feedback.answerPracticeQuestion;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AnswerPracticeQuestionRequest(
        @NotEmpty(message = "{feedback.alternatives.required}") List<Integer> alternativeIds
) {}

package br.com.naheroback.modules.practiceExams.useCases.questionBank.answer;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AnswerQuestionBankQuestionRequest(
        @NotEmpty(message = "{feedback.alternatives.required}") @Size(max = 10) List<Integer> alternativeIds
) {}

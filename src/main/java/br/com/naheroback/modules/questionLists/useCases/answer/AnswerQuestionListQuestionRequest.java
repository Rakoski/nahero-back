package br.com.naheroback.modules.questionLists.useCases.answer;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AnswerQuestionListQuestionRequest(
        @NotEmpty(message = "{feedback.alternatives.required}") @Size(max = 10) List<Integer> alternativeIds
) {}

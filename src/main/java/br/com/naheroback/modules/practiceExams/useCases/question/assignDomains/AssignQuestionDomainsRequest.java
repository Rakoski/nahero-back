package br.com.naheroback.modules.practiceExams.useCases.question.assignDomains;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignQuestionDomainsRequest(
        @NotNull @Positive Integer questionId,
        @NotBlank String domain
) {}

package br.com.naheroback.modules.practiceExams.useCases.questionBank.list;

import br.com.naheroback.modules.practiceExams.entities.enums.QuestionBankStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ListQuestionBankRequest(
        @Positive Integer practiceExamId,
        @Positive Integer domainId,
        QuestionBankStatus status,
        @PositiveOrZero Integer page,
        @Positive Integer size
) {}

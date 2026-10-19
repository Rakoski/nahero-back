package br.com.naheroback.modules.questionLists.useCases.create;

import br.com.naheroback.modules.practiceExams.entities.enums.QuestionBankStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateQuestionListRequest(
        @NotBlank @Size(max = 120) String name,
        List<@NotNull @Positive Integer> questionIds,
        @Valid Filter filter
) {
    public record Filter(
            @Positive Integer practiceExamId,
            @Positive Integer domainId,
            QuestionBankStatus status,
            @Positive Integer limit
    ) {}
}

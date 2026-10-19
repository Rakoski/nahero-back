package br.com.naheroback.modules.questionLists.useCases.addQuestions;

import br.com.naheroback.modules.practiceExams.entities.enums.QuestionBankStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record AddQuestionListQuestionsRequest(
        List<@NotNull @Positive Integer> questionIds,
        @Valid Filter filter
) {
    public record Filter(
            @Positive Integer practiceExamId,
            @Positive Integer domainId,
            QuestionBankStatus status
    ) {}
}

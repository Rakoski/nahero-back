package br.com.naheroback.modules.instructor.useCases.createQuestions;

import br.com.naheroback.modules.practiceExams.entities.enums.QuestionTypeEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateInstructorQuestionsRequest(
        @NotBlank @Pattern(regexp = "en|pt", message = "{instructor.language.invalid}") String language,
        @NotEmpty(message = "{instructor.questions.required}") @Size(max = 50, message = "{instructor.questions.too_many}")
        List<@Valid @NotNull QuestionRequest> questions
) {
    public record QuestionRequest(
            @NotNull(message = "{question.questiontypeid.required}") QuestionTypeEnum type,
            @NotNull(message = "{instructor.question.domain.required}") @Positive Integer domainId,
            @NotBlank(message = "{question.content.required}") @Size(max = 10000) String content,
            @Size(max = 10000) String explanation,
            @NotNull @Size(min = 2, max = 10, message = "{instructor.question.alternatives.size}")
            List<@Valid @NotNull AlternativeRequest> alternatives
    ) {
    }

    public record AlternativeRequest(
            @NotBlank(message = "{alternative.content.required}") @Size(max = 5000) String content,
            @NotNull(message = "{alternative.iscorrect.required}") Boolean isCorrect
    ) {
    }
}

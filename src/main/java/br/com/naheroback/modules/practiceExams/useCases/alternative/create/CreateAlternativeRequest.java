package br.com.naheroback.modules.practiceExams.useCases.alternative.create;

import br.com.naheroback.common.utils.ValidateNull;
import br.com.naheroback.modules.practiceExams.entities.Alternative;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.user.entities.User;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

public record CreateAlternativeRequest(
    Integer baseAlternativeId,
    @NotNull(message = "questionId is required") Integer questionId,
    @NotNull(message = "content is required") String content,
    @NotNull(message = "isCorrect is required") Boolean isCorrect,
    String imageUrl
) {
    public static Alternative toDomain(CreateAlternativeRequest request, int teacherId, int version) {
        Alternative alternative = new Alternative();

        if (Objects.nonNull(request.baseAlternativeId)) {
            alternative.setBaseAlternative(ValidateNull.validate(Alternative.class, request.baseAlternativeId));
        }

        alternative.setQuestion(ValidateNull.validate(Question.class, request.questionId));
        alternative.setTeacher(ValidateNull.validate(User.class, teacherId));
        alternative.setContent(request.content);
        alternative.setIsCorrect(request.isCorrect);
        alternative.setImageUrl(request.imageUrl);
        alternative.setVersion(version);
        return alternative;
    }
}

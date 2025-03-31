package br.com.naheroback.modules.practiceExams.useCases.question.create;

import br.com.naheroback.common.utils.ValidateNull;
import br.com.naheroback.modules.practiceExams.entities.Alternative;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.entities.QuestionType;
import br.com.naheroback.modules.user.entities.User;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record CreateQuestionRequest(
        Integer baseQuestionId,
        @NotNull(message = "practiceExamId is required") @Positive Integer practiceExamId,
        @NotNull(message = "questionTypeId is required") @Positive Integer questionTypeId,
        @NotNull(message = "content is required") String content,
        String imageUrl,
        String explanation,
        Integer points,
        List<AlternativeRequest> alternatives
) {

    public record AlternativeRequest(
            Integer baseAlternativeId,
            @NotNull(message = "questionId is required") Integer questionId,
            @NotNull(message = "content is required") String content,
            @NotNull(message = "isCorrect is required") Boolean isCorrect,
            String imageUrl
    ) {
        public static Alternative toDomain(AlternativeRequest request, Question question, int version) {
            Alternative alternative = new Alternative();

            if (Objects.nonNull(request.baseAlternativeId)) {
                alternative.setBaseAlternative(ValidateNull.validate(Alternative.class, request.baseAlternativeId));
            }

            alternative.setQuestion(question);
            alternative.setContent(request.content);
            alternative.setIsCorrect(request.isCorrect);
            alternative.setImageUrl(request.imageUrl);
            alternative.setIsActive(true);
            alternative.setVersion(version);

            return alternative;
        }
    }

    public static Question toDomain(CreateQuestionRequest request, int teacherId, int version) {
        Question question = new Question();

        if (Objects.nonNull(request.baseQuestionId)) {
            question.setBaseQuestion(ValidateNull.validate(Question.class, request.baseQuestionId));
        }

        question.setPracticeExam(ValidateNull.validate(PracticeExam.class, request.practiceExamId));
        question.setQuestionType(ValidateNull.validate(QuestionType.class, request.questionTypeId));
        question.setTeacher(ValidateNull.validate(User.class, teacherId));
        question.setIsActive(true);
        question.setContent(request.content);
        question.setImageUrl(request.imageUrl);
        question.setExplanation(request.explanation);
        question.setPoints(request.points);
        question.setVersion(version);

        return question;
    }

    public static List<Alternative> alternativesToDomain(CreateQuestionRequest request, Question question, int version) {
        if (request.alternatives == null || request.alternatives.isEmpty()) {
            return List.of();
        }

        return request.alternatives.stream()
                .map(alternativeRequest ->
                        AlternativeRequest.toDomain(alternativeRequest, question, version))
                .collect(Collectors.toList());
    }
}
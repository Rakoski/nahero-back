package br.com.naheroback.modules.practiceExams.useCases.answer.listAnswered;

import br.com.naheroback.modules.practiceExams.entities.Alternative;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.entities.StudentAnswer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListAnsweredAnswersResponse {

    private Integer studentAnswerId;
    private Integer studentPracticeAttemptId;
    private Integer questionId;
    private Integer questionVersion;
    private Integer selectedAlternativeId;
    private Integer selectedAlternativeVersion;
    private Boolean isCorrect;

    private String questionContent;
    private String questionImageUrl;
    private Integer questionPoints;
    private String questionType;
    private String explanation;

    private List<AlternativeResponse> alternatives;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlternativeResponse {
        private Integer alternativeId;
        private Integer alternativeVersion;
        private String content;
        private String imageUrl;
        private Boolean isCorrect;
        private Boolean isActive;

        public static AlternativeResponse fromEntity(Alternative alternative) {
            return AlternativeResponse.builder()
                    .alternativeId(alternative.getId())
                    .alternativeVersion(alternative.getVersion())
                    .content(alternative.getContent())
                    .imageUrl(alternative.getImageUrl())
                    .isCorrect(alternative.getIsCorrect())
                    .isActive(alternative.getIsActive())
                    .build();
        }
    }

    public static ListAnsweredAnswersResponse toPresentation(
            StudentAnswer answer,
            Question question,
            List<Alternative> alternatives) {

        return ListAnsweredAnswersResponse.builder()
                .studentAnswerId(answer.getId())
                .studentPracticeAttemptId(answer.getStudentPracticeAttempt().getId())
                .questionId(answer.getQuestionId())
                .questionVersion(answer.getQuestionVersion())
                .selectedAlternativeId(answer.getSelectedAlternativeId())
                .selectedAlternativeVersion(answer.getSelectedAlternativeVersion())
                .isCorrect(answer.getIsCorrect())
                .questionContent(question.getContent())
                .questionImageUrl(question.getImageUrl())
                .questionPoints(question.getPoints())
                .questionType(question.getQuestionType().getName())
                .explanation(question.getExplanation())
                .alternatives(alternatives.stream()
                        .map(AlternativeResponse::fromEntity)
                        .toList())
                .build();
    }
}


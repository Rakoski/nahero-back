package br.com.naheroback.modules.practiceExams.useCases.practiceExam.getSampleQuestions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetSampleQuestionsResponse {
    private Integer id;
    private String content;
    private String imageUrl;
    private String explanation;
    private String questionType;
    private List<GetSampleQuestionsAlternative> alternatives;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetSampleQuestionsAlternative {
        private Integer id;
        private String content;
        private Boolean isCorrect;
    }
}

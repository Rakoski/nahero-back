package br.com.naheroback.modules.practiceExams.useCases.practiceExam.getBySlug;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GetPracticeExamBySlugResponse {
    private Integer id;
    private String slug;
    private String title;
    private String description;
    private Integer passingScore;
    private Integer timeLimit;
    private Integer numberOfQuestions;
    private GetPracticeExamBySlugTeacher teacher;
    private GetPracticeExamBySlugExam exam;

    @Data
    @NoArgsConstructor
    public static class GetPracticeExamBySlugTeacher {
        private Integer id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    public static class GetPracticeExamBySlugExam {
        private Integer id;
        private String title;
        private Integer difficultyLevel;
    }
}

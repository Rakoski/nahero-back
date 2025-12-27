package br.com.naheroback.modules.practiceExams.useCases.practiceExam.list;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ListPracticeExamsResponse {
    private Integer id;
    private String title;
    private String description;
    private Integer passingScore;
    private Integer timeLimit;
    private ListPracticeExamsTeacher teacher;
    private ListPracticeExamsExam exam;

    @Data
    @NoArgsConstructor
    public static class ListPracticeExamsTeacher {
        private Integer id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    public static class ListPracticeExamsExam {
        private Integer id;
        private String title;
        private Integer difficultyLevel;
    }
}
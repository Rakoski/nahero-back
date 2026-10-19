package br.com.naheroback.modules.instructor.useCases.getPracticeExam;

import java.util.List;
import java.util.Map;

public record GetInstructorPracticeExamResponse(
        Integer id,
        String slug,
        String title,
        String description,
        Integer passingScore,
        Integer timeLimit,
        Integer numberOfQuestions,
        Boolean isActive,
        long questionCount,
        Map<String, Long> questionCountByLanguage,
        ExamSummary exam,
        List<DomainSummary> domains
) {
    public record ExamSummary(Integer id, String title, String category, Integer difficultyLevel) {
    }

    public record DomainSummary(Integer id, String name, Double minWeight, Double maxWeight, long questionCount) {
    }
}

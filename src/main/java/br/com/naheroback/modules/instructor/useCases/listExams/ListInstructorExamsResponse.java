package br.com.naheroback.modules.instructor.useCases.listExams;

import br.com.naheroback.modules.exams.entities.Exam;

public record ListInstructorExamsResponse(
        Integer id,
        String title,
        String description,
        String category,
        Integer difficultyLevel,
        long practiceExamCount
) {
    public static ListInstructorExamsResponse from(Exam exam, long practiceExamCount) {
        return new ListInstructorExamsResponse(
                exam.getId(),
                exam.getTitle(),
                exam.getDescription(),
                exam.getCategory(),
                exam.getDifficultyLevel(),
                practiceExamCount);
    }
}

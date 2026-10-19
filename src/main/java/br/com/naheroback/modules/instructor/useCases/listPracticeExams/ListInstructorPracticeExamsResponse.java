package br.com.naheroback.modules.instructor.useCases.listPracticeExams;

import br.com.naheroback.modules.practiceExams.entities.PracticeExam;

import java.time.LocalDateTime;

public record ListInstructorPracticeExamsResponse(
        Integer id,
        String slug,
        String title,
        Boolean isActive,
        Integer numberOfQuestions,
        long questionCount,
        Integer examId,
        String examTitle,
        LocalDateTime createdAt
) {
    public static ListInstructorPracticeExamsResponse from(PracticeExam practiceExam, long questionCount) {
        return new ListInstructorPracticeExamsResponse(
                practiceExam.getId(),
                practiceExam.getSlug(),
                practiceExam.getTitle(),
                practiceExam.getIsActive(),
                practiceExam.getNumberOfQuestions(),
                questionCount,
                practiceExam.getExam() == null ? null : practiceExam.getExam().getId(),
                practiceExam.getExam() == null ? null : practiceExam.getExam().getTitle(),
                practiceExam.getCreatedAt());
    }
}

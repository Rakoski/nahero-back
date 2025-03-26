package br.com.naheroback.modules.practice_exams.useCases.practiceExams.create;

import br.com.naheroback.common.utils.ValidateNull;
import br.com.naheroback.modules.exams.entities.Exam;
import br.com.naheroback.modules.practice_exams.entities.PracticeExam;
import br.com.naheroback.modules.user.entities.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePracticeExamRequest(
    @NotNull(message = "examId is required") @Positive Integer examId,
    @NotBlank(message = "title is required") String title,
    @NotBlank(message = "description is required") String description,
    @NotNull(message = "passingScore is required") @Positive Integer passingScore,
    Integer timeLimit,
    @NotNull(message = "difficultyLevel is required") Integer difficultyLevel
) {
    public static PracticeExam toDomain(CreatePracticeExamRequest input, User teacher) {
        PracticeExam practiceExam = new PracticeExam();
        practiceExam.setExam(ValidateNull.validate(Exam.class, input.examId));
        practiceExam.setTitle(input.title);
        practiceExam.setDescription(input.description);
        practiceExam.setPassingScore(input.passingScore);
        practiceExam.setTeacher(teacher);
        practiceExam.setTimeLimit(input.timeLimit);
        practiceExam.setDifficultyLevel(input.difficultyLevel);

        return practiceExam;
    }
}

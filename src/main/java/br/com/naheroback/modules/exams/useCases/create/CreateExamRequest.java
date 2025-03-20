package br.com.naheroback.modules.exams.useCases.create;

import br.com.naheroback.modules.exams.entities.Exam;
import br.com.naheroback.modules.user.entities.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateExamRequest(
    @NotBlank(message = "Title is required")
    String title,
    String description,
    String category,
    @Min(value = 1, message = "Difficulty level must be between 1 and 10")
    @Max(value = 5, message = "Difficulty level must be between 1 and 10")
    Integer difficultyLevel,
    Boolean isActive
) {
    public Exam toDomain(User teacher) {
        Exam exam = new Exam();
        exam.setTitle(title);
        exam.setDescription(description);
        exam.setCategory(category);
        exam.setDifficultyLevel(difficultyLevel);
        exam.setIsActive(isActive != null ? isActive : true);
        exam.setTeacher(teacher);
        return exam;
    }
}
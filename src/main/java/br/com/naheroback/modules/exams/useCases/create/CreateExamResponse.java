package br.com.naheroback.modules.exams.useCases.create;

import br.com.naheroback.modules.exams.entities.Exam;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Getter
@Setter
public class CreateExamResponse {
    private Integer id;
    private String title;
    private String description;
    private String category;
    private Integer difficultyLevel;
    private Boolean isActive;
    private Integer teacherId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CreateExamResponse toPresentation(Exam exam) {
        CreateExamResponse response = new CreateExamResponse();
        response.setId(exam.getId());
        response.setTitle(exam.getTitle());
        response.setDescription(exam.getDescription());
        response.setCategory(exam.getCategory());
        response.setDifficultyLevel(exam.getDifficultyLevel());
        response.setIsActive(exam.getIsActive());
        response.setTeacherId(exam.getTeacher().getId());
        response.setCreatedAt(exam.getCreatedAt());
        response.setUpdatedAt(exam.getUpdatedAt());
        return response;
    }
}
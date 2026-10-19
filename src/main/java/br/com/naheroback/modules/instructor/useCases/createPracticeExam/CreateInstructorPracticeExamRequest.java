package br.com.naheroback.modules.instructor.useCases.createPracticeExam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateInstructorPracticeExamRequest(
        @NotNull(message = "{practiceexam.examid.required}") @Positive Integer examId,
        @NotBlank(message = "{practiceexam.title.required}") @Size(max = 200) String title,
        @NotBlank(message = "{practiceexam.description.required}") @Size(max = 2000) String description,
        @NotNull(message = "{practiceexam.passingscore.required}") @Min(1) @Max(100) Integer passingScore,
        @NotNull(message = "{practiceexam.timelimit.required}") @Min(1) @Max(600) Integer timeLimit,
        @NotNull(message = "{practiceexam.numberofquestions.required}") @Min(1) @Max(500) Integer numberOfQuestions
) {
}

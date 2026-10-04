package br.com.naheroback.modules.practiceExams.services;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record StudyPlan(
        @NotBlank @Size(max = 160) String summary,
        @NotNull @Size(min = 1, max = 3) List<@Valid @NotNull Priority> priorities,
        @NotNull @Size(min = 3, max = 5) List<@NotBlank @Size(max = 70) String> plan
) {
    public record Priority(
            @NotBlank String domain,
            @NotBlank @Size(max = 110) String why,
            @NotBlank @Size(max = 90) String whatToStudy
    ) {}
}

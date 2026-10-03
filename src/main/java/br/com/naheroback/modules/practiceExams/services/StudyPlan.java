package br.com.naheroback.modules.practiceExams.services;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record StudyPlan(
        @NotBlank String summary,
        @NotNull @Size(min = 1, max = 3) List<@Valid @NotNull Priority> priorities,
        @NotNull @Size(min = 5, max = 7) List<@NotBlank String> plan
) {
    public record Priority(@NotBlank String domain, @NotBlank String why, @NotBlank String whatToStudy) {}
}

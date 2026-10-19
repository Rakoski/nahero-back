package br.com.naheroback.modules.instructor.useCases.updateStatus;

import jakarta.validation.constraints.NotNull;

public record UpdatePracticeExamStatusRequest(@NotNull Boolean active) {
}

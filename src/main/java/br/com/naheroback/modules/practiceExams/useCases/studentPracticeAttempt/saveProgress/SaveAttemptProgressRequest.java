package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.saveProgress;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record SaveAttemptProgressRequest(
        @Positive Integer questionId,
        List<@Positive Integer> alternativeIds,
        @PositiveOrZero Integer lastQuestionIndex
) {}

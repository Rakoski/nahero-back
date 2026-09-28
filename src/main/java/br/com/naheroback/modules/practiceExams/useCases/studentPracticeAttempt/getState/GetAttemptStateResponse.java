package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getState;

import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.entities.StudentAttemptAnswerDraft;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record GetAttemptStateResponse(
        Integer attemptId,
        Integer practiceExamId,
        String practiceExamSlug,
        String practiceExamTitle,
        String attemptStatus,
        LocalDateTime startTime,
        Integer timeLimit,
        long remainingSeconds,
        Integer lastQuestionIndex,
        Integer totalQuestions,
        Integer answeredCount,
        List<SavedAnswer> answers
) {
    public record SavedAnswer(Integer questionId, List<Integer> alternativeIds) {}

    public static GetAttemptStateResponse toPresentation(StudentPracticeAttempt attempt,
                                                        List<StudentAttemptAnswerDraft> drafts,
                                                        int totalQuestions) {
        PracticeExam practiceExam = attempt.getPracticeExam();

        List<SavedAnswer> answers = drafts.stream()
                .map(draft -> new SavedAnswer(draft.getQuestionId(), draft.getSelectedAlternativeIds()))
                .toList();

        return new GetAttemptStateResponse(
                attempt.getId(),
                practiceExam.getId(),
                practiceExam.getSlug(),
                practiceExam.getTitle(),
                attempt.getAttemptStatus().getName(),
                attempt.getStartTime(),
                practiceExam.getTimeLimit(),
                remainingSeconds(attempt),
                attempt.getLastQuestionIndex() != null ? attempt.getLastQuestionIndex() : 0,
                totalQuestions,
                answers.size(),
                answers
        );
    }

    private static long remainingSeconds(StudentPracticeAttempt attempt) {
        Integer timeLimit = attempt.getPracticeExam().getTimeLimit();

        if (attempt.getStartTime() == null || timeLimit == null) return 0;

        LocalDateTime deadline = attempt.getStartTime().plusMinutes(timeLimit);
        long remaining = Duration.between(LocalDateTime.now(), deadline).toSeconds();

        return Math.max(0, remaining);
    }
}

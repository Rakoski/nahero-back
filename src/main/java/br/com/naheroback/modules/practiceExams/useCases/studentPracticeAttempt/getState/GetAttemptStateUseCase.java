package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getState;

import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.services.AttemptAnswerDraftService;
import br.com.naheroback.modules.practiceExams.services.QuestionShuffleService;
import br.com.naheroback.modules.practiceExams.services.StudentAttemptAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetAttemptStateUseCase {
    private final StudentAttemptAccessService attemptAccess;
    private final AttemptAnswerDraftService draftService;
    private final QuestionShuffleService questionShuffleService;

    @Transactional(readOnly = true)
    @Secured("IS_STUDENT")
    public GetAttemptStateResponse execute(Integer attemptId) {
        StudentPracticeAttempt attempt = attemptAccess.loadOwnedAttempt(attemptId);

        return GetAttemptStateResponse.toPresentation(
                attempt,
                draftService.findAllByAttempt(attemptId),
                questionShuffleService.getMaxQuestionsForExam(attempt)
        );
    }
}

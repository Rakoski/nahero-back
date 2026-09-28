package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.saveProgress;

import br.com.naheroback.common.exceptions.custom.ConflictException;
import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.entities.enums.PracticeAttemptStatusesEnum;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import br.com.naheroback.modules.practiceExams.services.AttemptAnswerDraftService;
import br.com.naheroback.modules.practiceExams.services.QuestionShuffleService;
import br.com.naheroback.modules.practiceExams.services.StudentAttemptAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SaveAttemptProgressUseCase {
    public static final String NOT_IN_PROGRESS_CODE = "ATTEMPT_NOT_IN_PROGRESS";

    private final StudentPracticeAttemptRepository studentPracticeAttemptRepository;
    private final StudentAttemptAccessService attemptAccess;
    private final AttemptAnswerDraftService draftService;
    private final QuestionShuffleService questionShuffleService;

    @Transactional
    @Secured("IS_STUDENT")
    public void execute(Integer attemptId, SaveAttemptProgressRequest request) {
        StudentPracticeAttempt attempt = attemptAccess.loadOwnedAttemptForUpdate(attemptId);

        if (!Objects.equals(attempt.getAttemptStatus().getId(), PracticeAttemptStatusesEnum.IN_PROGRESS.getId())) {
            throw new ConflictException("attempt.not_in_progress", NOT_IN_PROGRESS_CODE);
        }

        if (request.questionId() != null) {
            if (!questionShuffleService.getShuffledQuestionIds(attemptId).contains(request.questionId())) {
                throw NotFoundException.with(Question.class, "id", request.questionId());
            }

            draftService.saveAnswer(attempt, request.questionId(), request.alternativeIds());
        }

        if (request.lastQuestionIndex() != null) {
            attempt.setLastQuestionIndex(request.lastQuestionIndex());
            studentPracticeAttemptRepository.save(attempt);
        }
    }
}

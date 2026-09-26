package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.timeout;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.practiceExams.entities.PracticeAttemptStatus;
import br.com.naheroback.modules.practiceExams.entities.StudentAnswer;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.entities.enums.PracticeAttemptStatusesEnum;
import br.com.naheroback.modules.practiceExams.repositories.PracticeAttemptStatusRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentAnswerRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import br.com.naheroback.modules.practiceExams.services.AttemptAnswerDraftService;
import br.com.naheroback.modules.practiceExams.services.AttemptScoringService;
import br.com.naheroback.modules.practiceExams.services.PracticeAttemptEntitlementService;
import br.com.naheroback.modules.practiceExams.services.StudentAttemptAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeOutStudentPracticeAttemptUseCase {
    private final StudentPracticeAttemptRepository studentPracticeAttemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final PracticeAttemptStatusRepository practiceAttemptStatusRepository;
    private final AttemptScoringService attemptScoringService;
    private final AttemptAnswerDraftService draftService;
    private final StudentAttemptAccessService attemptAccess;
    private final PracticeAttemptEntitlementService entitlement;

    @Transactional
    public void execute(Integer attemptId, TimeOutStudentPracticeAttemptRequest request) {
        StudentPracticeAttempt attempt = attemptAccess.loadOwnedAttemptForUpdate(attemptId);

        if (!Objects.equals(attempt.getAttemptStatus().getId(), PracticeAttemptStatusesEnum.IN_PROGRESS.getId())) {
            log.info("Timeout requested on attempt {} already in terminal status {} — no-op",
                    attempt.getId(), attempt.getAttemptStatus().getId());
            return;
        }

        markTimedOut(attempt);

        List<AttemptScoringService.AnswerData> submitted =
                (request.answers() == null ? List.<TimeOutStudentPracticeAttemptRequest.AnswerRequest>of() : request.answers())
                .stream()
                .map(answer -> new AttemptScoringService.AnswerData(
                        answer.questionId(),
                        answer.alternativeIds(),
                        answer.descriptiveAnswer(),
                        answer.sumAnswer()))
                .toList();

        List<StudentAnswer> answers = attemptScoringService.scoreAttempt(
                attempt, draftService.mergeWithSubmitted(attempt.getId(), submitted));

        studentPracticeAttemptRepository.save(attempt);
        studentAnswerRepository.saveAll(answers);
        entitlement.consumeFreeTryOnCompletion(attempt);
    }

    private void markTimedOut(StudentPracticeAttempt attempt) {
        Integer timedOutStatusId = PracticeAttemptStatusesEnum.TIMED_OUT.getId();
        PracticeAttemptStatus timedOutStatus = practiceAttemptStatusRepository.findById(timedOutStatusId)
                .orElseThrow(() -> NotFoundException.with(PracticeAttemptStatus.class, "id", timedOutStatusId));

        attempt.setAttemptStatus(timedOutStatus);
        attempt.setEndTime(LocalDateTime.now());
    }
}

package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.abandon;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.practiceExams.entities.PracticeAttemptStatus;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.entities.enums.PracticeAttemptStatusesEnum;
import br.com.naheroback.modules.practiceExams.repositories.PracticeAttemptStatusRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AbandonStudentPracticeAttemptUseCase {
    private final StudentPracticeAttemptRepository studentPracticeAttemptRepository;
    private final PracticeAttemptStatusRepository practiceAttemptStatusRepository;

    @Transactional
    public void execute(Integer attemptId) {
        StudentPracticeAttempt attempt = studentPracticeAttemptRepository.findById(attemptId)
                .orElseThrow(() -> NotFoundException.with(StudentPracticeAttempt.class, "id", attemptId));

        if (!Objects.equals(attempt.getAttemptStatus().getId(), PracticeAttemptStatusesEnum.IN_PROGRESS.getId())) {
            log.info("Abandon requested on attempt {} already in terminal status {} — no-op",
                    attempt.getId(), attempt.getAttemptStatus().getId());
            return;
        }

        Integer abandonedStatusId = PracticeAttemptStatusesEnum.ABANDONED.getId();
        PracticeAttemptStatus abandonedStatus = practiceAttemptStatusRepository.findById(abandonedStatusId)
                .orElseThrow(() -> NotFoundException.with(PracticeAttemptStatus.class, "id", abandonedStatusId));

        attempt.setAttemptStatus(abandonedStatus);
        attempt.setEndTime(LocalDateTime.now());

        studentPracticeAttemptRepository.save(attempt);
    }
}

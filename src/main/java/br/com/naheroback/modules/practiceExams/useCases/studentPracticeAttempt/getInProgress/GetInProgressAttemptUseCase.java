package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getInProgress;

import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.entities.enums.PracticeAttemptStatusesEnum;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.abandon.AbandonStudentPracticeAttemptUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getState.GetAttemptStateResponse;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getState.GetAttemptStateUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetInProgressAttemptUseCase {
    private final StudentPracticeAttemptRepository studentPracticeAttemptRepository;
    private final GetAttemptStateUseCase getAttemptStateUseCase;
    private final AbandonStudentPracticeAttemptUseCase abandonStudentPracticeAttemptUseCase;

    @Transactional
    @Secured("IS_STUDENT")
    public Optional<GetAttemptStateResponse> execute() {
        Integer studentId = AuthService.getUserFromToken().getId();

        List<StudentPracticeAttempt> inProgress = studentPracticeAttemptRepository.findByStudentAndStatus(
                studentId, PracticeAttemptStatusesEnum.IN_PROGRESS.getId());

        if (inProgress.isEmpty()) return Optional.empty();

        LocalDateTime now = LocalDateTime.now();

        Optional<StudentPracticeAttempt> resumable = inProgress.stream()
                .filter(attempt -> !hasRunOutOfTime(attempt, now))
                .findFirst();

        abandonEverythingElse(inProgress, resumable, studentId);

        return resumable.map(attempt -> getAttemptStateUseCase.execute(attempt.getId()));
    }

    private boolean hasRunOutOfTime(StudentPracticeAttempt attempt, LocalDateTime now) {
        Integer timeLimit = attempt.getPracticeExam().getTimeLimit();

        if (attempt.getStartTime() == null || timeLimit == null) return false;

        return now.isAfter(attempt.getStartTime().plusMinutes(timeLimit));
    }

    private void abandonEverythingElse(List<StudentPracticeAttempt> inProgress, Optional<StudentPracticeAttempt> resumable, Integer studentId) {
        List<StudentPracticeAttempt> stale = inProgress.stream()
                .filter(attempt -> resumable.isEmpty() || !attempt.getId().equals(resumable.get().getId()))
                .toList();

        if (stale.isEmpty()) return;

        stale.forEach(attempt -> abandonStudentPracticeAttemptUseCase.execute(attempt.getId()));
    }
}

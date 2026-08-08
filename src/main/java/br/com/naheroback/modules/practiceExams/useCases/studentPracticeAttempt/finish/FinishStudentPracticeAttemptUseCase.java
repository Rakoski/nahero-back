package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.finish;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.practiceExams.entities.*;
import br.com.naheroback.modules.practiceExams.entities.enums.PracticeAttemptStatusesEnum;
import br.com.naheroback.modules.practiceExams.repositories.PracticeAttemptStatusRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentAnswerRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import br.com.naheroback.modules.practiceExams.services.AttemptScoringService;
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
public class FinishStudentPracticeAttemptUseCase {
    private final StudentPracticeAttemptRepository studentPracticeAttemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final PracticeAttemptStatusRepository practiceAttemptStatusRepository;
    private final AttemptScoringService attemptScoringService;

    @Transactional
    public void execute(FinishStudentPracticeAttemptRequest request) {
        StudentPracticeAttempt attempt = studentPracticeAttemptRepository.findById(request.studentPracticeAttemptId())
                .orElseThrow(() -> NotFoundException.with(StudentPracticeAttempt.class, "id", request.studentPracticeAttemptId()));

        if (!Objects.equals(attempt.getAttemptStatus().getId(), PracticeAttemptStatusesEnum.IN_PROGRESS.getId())) {
            log.info("Finish requested on attempt {} already in terminal status {} — no-op", attempt.getId(), attempt.getAttemptStatus().getId());
            return;
        }

        updateAttemptStatus(attempt);

        List<AttemptScoringService.AnswerData> answerData = request.answers().stream()
                .map(answer -> new AttemptScoringService.AnswerData(
                        answer.questionId(),
                        answer.alternativeIds(),
                        answer.descriptiveAnswer(),
                        answer.sumAnswer()))
                .toList();

        List<StudentAnswer> answers = attemptScoringService.scoreAttempt(attempt, answerData);

        studentPracticeAttemptRepository.save(attempt);
        studentAnswerRepository.saveAll(answers);
    }

    private void updateAttemptStatus(StudentPracticeAttempt attempt) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxEndTime = attempt.getStartTime().plusMinutes(attempt.getPracticeExam().getTimeLimit());

        boolean isTimedOut = now.isAfter(maxEndTime);
        Integer statusId = isTimedOut
                ? PracticeAttemptStatusesEnum.TIMED_OUT.getId()
                : PracticeAttemptStatusesEnum.COMPLETED.getId();

        PracticeAttemptStatus newStatus = practiceAttemptStatusRepository.findById(statusId)
                .orElseThrow(() -> NotFoundException.with(PracticeAttemptStatus.class, "id", statusId));

        attempt.setAttemptStatus(newStatus);
        attempt.setEndTime(now);
    }
}

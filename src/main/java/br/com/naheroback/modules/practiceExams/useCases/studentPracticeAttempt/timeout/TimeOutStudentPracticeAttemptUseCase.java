package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.timeout;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.common.exceptions.custom.ValidationException;
import br.com.naheroback.modules.practiceExams.entities.PracticeAttemptStatus;
import br.com.naheroback.modules.practiceExams.entities.StudentAnswer;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.entities.enums.PracticeAttemptStatusesEnum;
import br.com.naheroback.modules.practiceExams.repositories.PracticeAttemptStatusRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentAnswerRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import br.com.naheroback.modules.practiceExams.services.AttemptScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TimeOutStudentPracticeAttemptUseCase {
    private final StudentPracticeAttemptRepository studentPracticeAttemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final PracticeAttemptStatusRepository practiceAttemptStatusRepository;
    private final AttemptScoringService attemptScoringService;

    @Transactional
    public void execute(Integer attemptId, TimeOutStudentPracticeAttemptRequest request) {
        StudentPracticeAttempt attempt = validateAndRetrieveAttempt(attemptId);

        markTimedOut(attempt);

        List<AttemptScoringService.AnswerData> answerData = (request.answers() == null ? List.<TimeOutStudentPracticeAttemptRequest.AnswerRequest>of() : request.answers())
                .stream()
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

    private StudentPracticeAttempt validateAndRetrieveAttempt(Integer attemptId) {
        StudentPracticeAttempt attempt = studentPracticeAttemptRepository.findById(attemptId)
                .orElseThrow(() -> NotFoundException.with(StudentPracticeAttempt.class, "id", attemptId));

        if (!Objects.equals(attempt.getAttemptStatus().getId(), PracticeAttemptStatusesEnum.IN_PROGRESS.getId())) {
            throw new ValidationException("Cannot time out an attempt that is not in progress");
        }

        return attempt;
    }

    private void markTimedOut(StudentPracticeAttempt attempt) {
        Integer timedOutStatusId = PracticeAttemptStatusesEnum.TIMED_OUT.getId();
        PracticeAttemptStatus timedOutStatus = practiceAttemptStatusRepository.findById(timedOutStatusId)
                .orElseThrow(() -> NotFoundException.with(PracticeAttemptStatus.class, "id", timedOutStatusId));

        attempt.setAttemptStatus(timedOutStatus);
        attempt.setEndTime(LocalDateTime.now());
    }
}

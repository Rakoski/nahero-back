package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getResult;

import br.com.naheroback.modules.practiceExams.entities.StudentAnswer;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.repositories.StudentAnswerRepository;
import br.com.naheroback.modules.practiceExams.services.StudentAttemptAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetResultUseCase {
    private final StudentAttemptAccessService attemptAccess;
    private final StudentAnswerRepository studentAnswerRepository;

    @Transactional(readOnly = true)
    public GetResultResponse execute(Integer studentPracticeAttemptId) {
        StudentPracticeAttempt attempt = attemptAccess.loadOwnedAttempt(studentPracticeAttemptId);

        List<StudentAnswer> answers = studentAnswerRepository.findAllByStudentPracticeAttemptId(studentPracticeAttemptId);

        return GetResultResponse.toPresentation(attempt, answers);
    }
}

package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getResult;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.practiceExams.entities.StudentAnswer;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.repositories.StudentAnswerRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetResultUseCase {
    private final StudentPracticeAttemptRepository studentPracticeAttemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;

    @Transactional(readOnly = true)
    public GetResultResponse execute(Integer studentPracticeAttemptId) {
        StudentPracticeAttempt attempt = studentPracticeAttemptRepository.findById(studentPracticeAttemptId)
                .orElseThrow(() -> NotFoundException.with(StudentPracticeAttempt.class, "id", studentPracticeAttemptId));

        List<StudentAnswer> answers = studentAnswerRepository.findAllByStudentPracticeAttemptId(studentPracticeAttemptId);

        return GetResultResponse.toPresentation(attempt, answers);
    }
}

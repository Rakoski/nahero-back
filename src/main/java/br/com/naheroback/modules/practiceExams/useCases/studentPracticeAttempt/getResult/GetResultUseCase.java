package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getResult;

import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.entities.StudentAnswer;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentAnswerRepository;
import br.com.naheroback.modules.practiceExams.services.StudentAttemptAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetResultUseCase {
    private final StudentAttemptAccessService attemptAccess;
    private final StudentAnswerRepository studentAnswerRepository;
    private final QuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public GetResultResponse execute(Integer studentPracticeAttemptId) {
        StudentPracticeAttempt attempt = attemptAccess.loadOwnedAttempt(studentPracticeAttemptId);

        List<StudentAnswer> answers = studentAnswerRepository.findAllByStudentPracticeAttemptId(studentPracticeAttemptId);

        return GetResultResponse.toPresentation(attempt, answers, resolveDomains(answers));
    }

    private Map<Integer, String> resolveDomains(List<StudentAnswer> answers) {
        List<Integer> questionIds = answers.stream()
                .map(StudentAnswer::getQuestionId)
                .distinct()
                .toList();
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        return questionRepository.findAllByIdIn(questionIds).stream()
                .filter(question -> question.getDomain() != null)
                .collect(Collectors.toMap(Question::getId, Question::getDomain));
    }
}

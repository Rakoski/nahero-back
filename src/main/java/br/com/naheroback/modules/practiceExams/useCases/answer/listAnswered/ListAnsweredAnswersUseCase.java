package br.com.naheroback.modules.practiceExams.useCases.answer.listAnswered;

import br.com.naheroback.modules.practiceExams.entities.Alternative;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.entities.StudentAnswer;
import br.com.naheroback.modules.practiceExams.repositories.AlternativeRepository;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentAnswerRepository;
import br.com.naheroback.modules.practiceExams.services.PracticeAttemptEntitlementService;
import br.com.naheroback.modules.practiceExams.services.StudentAttemptAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListAnsweredAnswersUseCase {
    private final StudentAnswerRepository studentAnswerRepository;
    private final StudentAttemptAccessService attemptAccess;
    private final QuestionRepository questionRepository;
    private final AlternativeRepository alternativeRepository;
    private final PracticeAttemptEntitlementService entitlement;

    @Transactional(readOnly = true)
    public Page<ListAnsweredAnswersResponse> execute(
            Integer studentPracticeAttemptId,
            AnswerFilterDTO filter,
            Pageable pageable) {
        attemptAccess.loadOwnedAttempt(studentPracticeAttemptId);

        boolean includeExplanation = entitlement.canSeeExplanations();

        Page<StudentAnswer> answersPage = studentAnswerRepository.findByAttemptIdWithFilters(
                studentPracticeAttemptId,
                filter.getIsCorrect(),
                filter.getQuestionContent(),
                pageable
        );

        List<Integer> questionIds = answersPage.getContent().stream()
                .map(StudentAnswer::getQuestionId)
                .distinct()
                .toList();

        Map<Integer, Question> questionsMap = questionRepository.findAllByIdIn(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        Map<Integer, List<Alternative>> alternativesMap = questionIds.stream()
                .collect(Collectors.toMap(
                        qId -> qId,
                        alternativeRepository::findAllByQuestionId
                ));

        return answersPage.map(answer -> {
            Question question = questionsMap.get(answer.getQuestionId());
            List<Alternative> alternatives = alternativesMap.getOrDefault(answer.getQuestionId(), List.of());
            return ListAnsweredAnswersResponse.toPresentation(answer, question, alternatives, includeExplanation);
        });
    }
}

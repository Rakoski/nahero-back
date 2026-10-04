package br.com.naheroback.modules.practiceExams.useCases.feedback.answerPracticeQuestion;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practiceExams.entities.Alternative;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.entities.enums.QuestionTypeEnum;
import br.com.naheroback.modules.practiceExams.repositories.AlternativeRepository;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import br.com.naheroback.modules.practiceExams.services.AttemptScoringService;
import br.com.naheroback.modules.practiceExams.services.PracticeAttemptEntitlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnswerPracticeQuestionUseCase {
    private static final Set<QuestionTypeEnum> CHOICE_TYPES =
            Set.of(QuestionTypeEnum.MULTIPLE_CHOICE, QuestionTypeEnum.TRUE_FALSE, QuestionTypeEnum.OBJECTIVE);

    private final QuestionRepository questionRepository;
    private final AlternativeRepository alternativeRepository;
    private final StudentPracticeAttemptRepository studentPracticeAttemptRepository;
    private final PracticeAttemptEntitlementService entitlement;

    @Transactional(readOnly = true)
    public AnswerPracticeQuestionResponse execute(Integer questionId, AnswerPracticeQuestionRequest request) {
        entitlement.ensureCanSeeFeedback();

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> NotFoundException.with(Question.class, "id", questionId));

        QuestionTypeEnum type = QuestionTypeEnum.fromId(question.getQuestionType().getId());
        boolean takenByStudent = studentPracticeAttemptRepository.existsScored(
                AuthService.getUserFromToken().getId(), question.getPracticeExam().getId());

        if (!CHOICE_TYPES.contains(type) || !takenByStudent) {
            throw NotFoundException.with(Question.class, "id", questionId);
        }

        List<Integer> correctIds = alternativeRepository.findAllActiveByQuestionIdIn(List.of(questionId)).stream()
                .filter(alternative -> Boolean.TRUE.equals(alternative.getIsCorrect()))
                .map(Alternative::getId)
                .toList();

        boolean correct = AttemptScoringService.determineIfCorrect(request.alternativeIds(), correctIds, type);

        return new AnswerPracticeQuestionResponse(correct, correctIds, question.getExplanation());
    }
}

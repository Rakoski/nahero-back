package br.com.naheroback.modules.questionLists.useCases.answer;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.services.PracticeAttemptEntitlementService;
import br.com.naheroback.modules.practiceExams.services.QuestionBankThrottle;
import br.com.naheroback.modules.practiceExams.services.StudyAnswerService;
import br.com.naheroback.modules.questionLists.repositories.QuestionListItemRepository;
import br.com.naheroback.modules.questionLists.services.QuestionListService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnswerQuestionListQuestionUseCase {
    private final QuestionListService questionListService;
    private final QuestionListItemRepository questionListItemRepository;
    private final StudyAnswerService studyAnswerService;
    private final PracticeAttemptEntitlementService entitlement;
    private final QuestionBankThrottle throttle;

    @Transactional
    public AnswerQuestionListQuestionResponse execute(Integer questionListId, Integer questionId, AnswerQuestionListQuestionRequest request, String captchaToken) {
        entitlement.ensureCanUseQuestionBank();
        Integer studentId = AuthService.getUserFromToken().getId();
        throttle.check(studentId, captchaToken);

        questionListService.loadOwned(questionListId, studentId);
        if (!questionListItemRepository.existsByQuestionListIdAndQuestionId(questionListId, questionId)) {
            throw NotFoundException.with(Question.class, "id", questionId);
        }

        StudyAnswerService.Graded graded = studyAnswerService.answer(studentId, questionId, request.alternativeIds(), questionListId);
        return new AnswerQuestionListQuestionResponse(graded.correct(), graded.correctAlternativeIds(), graded.explanation());
    }
}

package br.com.naheroback.modules.questionLists.useCases.removeQuestion;

import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practiceExams.services.PracticeAttemptEntitlementService;
import br.com.naheroback.modules.questionLists.repositories.QuestionListItemRepository;
import br.com.naheroback.modules.questionLists.services.QuestionListService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RemoveQuestionListQuestionUseCase {
    private final QuestionListService questionListService;
    private final QuestionListItemRepository questionListItemRepository;
    private final PracticeAttemptEntitlementService entitlement;

    @Transactional
    public void execute(Integer questionListId, Integer questionId) {
        entitlement.ensureCanUseQuestionBank();
        Integer studentId = AuthService.getUserFromToken().getId();
        questionListService.loadOwned(questionListId, studentId);
        questionListItemRepository.deleteByQuestionListIdAndQuestionId(questionListId, questionId);
    }
}

package br.com.naheroback.modules.questionLists.useCases.delete;

import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practiceExams.services.PracticeAttemptEntitlementService;
import br.com.naheroback.modules.questionLists.repositories.QuestionListRepository;
import br.com.naheroback.modules.questionLists.services.QuestionListService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteQuestionListUseCase {
    private final QuestionListRepository questionListRepository;
    private final QuestionListService questionListService;
    private final PracticeAttemptEntitlementService entitlement;

    @Transactional
    public void execute(Integer questionListId) {
        entitlement.ensureCanUseQuestionBank();
        Integer studentId = AuthService.getUserFromToken().getId();

        questionListRepository.delete(questionListService.loadOwned(questionListId, studentId));
    }
}

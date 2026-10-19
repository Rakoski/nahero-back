package br.com.naheroback.modules.practiceExams.useCases.questionBank.answer;

import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practiceExams.services.PracticeAttemptEntitlementService;
import br.com.naheroback.modules.practiceExams.services.QuestionBankThrottle;
import br.com.naheroback.modules.practiceExams.services.StudyAnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnswerQuestionBankQuestionUseCase {
    private final StudyAnswerService studyAnswerService;
    private final PracticeAttemptEntitlementService entitlement;
    private final QuestionBankThrottle throttle;

    @Transactional
    public AnswerQuestionBankQuestionResponse execute(Integer questionId,
                                                      AnswerQuestionBankQuestionRequest request,
                                                      String captchaToken) {
        entitlement.ensureCanUseQuestionBank();
        Integer studentId = AuthService.getUserFromToken().getId();
        throttle.check(studentId, captchaToken);

        StudyAnswerService.Graded graded = studyAnswerService.answer(studentId, questionId, request.alternativeIds(), null);
        return new AnswerQuestionBankQuestionResponse(graded.correct(), graded.correctAlternativeIds(), graded.explanation());
    }
}

package br.com.naheroback.modules.practiceExams.useCases.questionBank.get;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.common.utils.ShuffleUtils;
import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.entities.enums.QuestionTypeEnum;
import br.com.naheroback.modules.practiceExams.repositories.AlternativeRepository;
import br.com.naheroback.modules.practiceExams.repositories.QuestionBankRepository;
import br.com.naheroback.modules.practiceExams.services.PracticeAttemptEntitlementService;
import br.com.naheroback.modules.practiceExams.services.QuestionBankThrottle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetQuestionBankQuestionUseCase {
    private final QuestionBankRepository questionBankRepository;
    private final AlternativeRepository alternativeRepository;
    private final PracticeAttemptEntitlementService entitlement;
    private final QuestionBankThrottle throttle;

    @Transactional(readOnly = true)
    public GetQuestionBankQuestionResponse execute(Integer questionId, String captchaToken) {
        entitlement.ensureCanUseQuestionBank();
        Integer studentId = AuthService.getUserFromToken().getId();
        throttle.check(studentId, captchaToken);

        Question question = questionBankRepository.findVisible(questionId)
                .orElseThrow(() -> NotFoundException.with(Question.class, "id", questionId));
        boolean answered = questionBankRepository.findAnsweredAmong(studentId, List.of(questionId)).contains(questionId);

        List<GetQuestionBankQuestionResponse.AlternativeOption> alternatives = ShuffleUtils.shuffleDifferentOrder(
                        alternativeRepository.findAllActiveByQuestionIdIn(List.of(questionId))).stream()
                .map(alternative -> new GetQuestionBankQuestionResponse.AlternativeOption(
                        alternative.getId(), alternative.getContent(), alternative.getImageUrl()))
                .toList();

        return new GetQuestionBankQuestionResponse(
                question.getId(),
                question.getContent(),
                question.getImageUrl(),
                QuestionTypeEnum.fromId(question.getQuestionType().getId()).name(),
                answered,
                new GetQuestionBankQuestionResponse.PracticeExamSummary(
                        question.getPracticeExam().getId(),
                        question.getPracticeExam().getSlug(),
                        question.getPracticeExam().getTitle()),
                question.getDomain() == null ? null : new GetQuestionBankQuestionResponse.DomainSummary(
                        question.getDomain().getId(), question.getDomain().getName()),
                alternatives);
    }
}

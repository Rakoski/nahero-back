package br.com.naheroback.modules.questionLists.useCases.get;

import br.com.naheroback.common.utils.StringUtils;
import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.entities.enums.QuestionTypeEnum;
import br.com.naheroback.modules.practiceExams.repositories.QuestionBankRepository;
import br.com.naheroback.modules.practiceExams.services.PracticeAttemptEntitlementService;
import br.com.naheroback.modules.practiceExams.services.QuestionBankProperties;
import br.com.naheroback.modules.practiceExams.services.QuestionBankThrottle;
import br.com.naheroback.modules.questionLists.entities.QuestionList;
import br.com.naheroback.modules.questionLists.services.QuestionListService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GetQuestionListUseCase {
    private final QuestionListService questionListService;
    private final QuestionBankRepository questionBankRepository;
    private final PracticeAttemptEntitlementService entitlement;
    private final QuestionBankThrottle throttle;
    private final QuestionBankProperties properties;

    @Transactional(readOnly = true)
    public GetQuestionListResponse execute(Integer questionListId, Integer page, Integer size, String captchaToken) {
        entitlement.ensureCanUseQuestionBank();
        Integer studentId = AuthService.getUserFromToken().getId();
        throttle.check(studentId, captchaToken);

        QuestionList questionList = questionListService.loadOwned(questionListId, studentId);
        List<Question> questions = questionListService.visibleQuestions(questionListId);
        Set<Integer> answered = questionBankRepository.findAnsweredAmong(
                studentId, questions.stream().map(Question::getId).toList());

        int pageIndex = page == null ? 0 : page;
        int pageSize = size == null ? properties.getMaxPageSize() : Math.min(size, properties.getMaxPageSize());
        int from = (int) Math.min((long) pageIndex * pageSize, questions.size());
        int to = Math.min(from + pageSize, questions.size());

        return new GetQuestionListResponse(
                questionList.getId(),
                questionList.getName(),
                questions.size(),
                (int) questions.stream().filter(question -> answered.contains(question.getId())).count(),
                questions.subList(from, to).stream()
                        .map(question -> toItem(question, answered.contains(question.getId())))
                        .toList(),
                pageIndex,
                pageSize,
                to < questions.size());
    }

    private GetQuestionListResponse.Item toItem(Question question, boolean answered) {
        return new GetQuestionListResponse.Item(
                question.getId(),
                StringUtils.plainText(question.getContent(), properties.getPreviewLength()),
                QuestionTypeEnum.fromId(question.getQuestionType().getId()).name(),
                answered,
                new GetQuestionListResponse.PracticeExamSummary(
                        question.getPracticeExam().getId(),
                        question.getPracticeExam().getSlug(),
                        question.getPracticeExam().getTitle()),
                question.getDomain() == null ? null : new GetQuestionListResponse.DomainSummary(
                        question.getDomain().getId(), question.getDomain().getName()));
    }
}

package br.com.naheroback.modules.practiceExams.useCases.questionBank.list;

import br.com.naheroback.common.utils.StringUtils;
import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.entities.enums.QuestionTypeEnum;
import br.com.naheroback.modules.practiceExams.repositories.QuestionBankRepository;
import br.com.naheroback.modules.practiceExams.services.PracticeAttemptEntitlementService;
import br.com.naheroback.modules.practiceExams.services.QuestionBankProperties;
import br.com.naheroback.modules.practiceExams.services.QuestionBankThrottle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ListQuestionBankUseCase {
    private final QuestionBankRepository questionBankRepository;
    private final PracticeAttemptEntitlementService entitlement;
    private final QuestionBankThrottle throttle;
    private final QuestionBankProperties properties;

    @Transactional(readOnly = true)
    public ListQuestionBankResponse execute(ListQuestionBankRequest request, Locale locale, String captchaToken) {
        entitlement.ensureCanUseQuestionBank();
        Integer studentId = AuthService.getUserFromToken().getId();
        throttle.check(studentId, captchaToken);

        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null
                ? properties.getMaxPageSize()
                : Math.min(request.size(), properties.getMaxPageSize());
        String language = locale == null ? Locale.ENGLISH.getLanguage() : locale.getLanguage();

        QuestionBankRepository.Filter filter = new QuestionBankRepository.Filter(
                studentId, language, request.practiceExamId(), request.domainId(), request.status());
        List<Question> fetched = questionBankRepository.findPage(filter, (long) page * size, size + 1);

        boolean hasNext = fetched.size() > size;
        List<Question> questions = hasNext ? fetched.subList(0, size) : fetched;
        Set<Integer> answered = questionBankRepository.findAnsweredAmong(
                studentId, questions.stream().map(Question::getId).toList());

        return new ListQuestionBankResponse(
                questions.stream().map(question -> toItem(question, answered.contains(question.getId()))).toList(),
                page,
                size,
                hasNext);
    }

    private ListQuestionBankResponse.Item toItem(Question question, boolean answered) {
        return new ListQuestionBankResponse.Item(
                question.getId(),
                StringUtils.plainText(question.getContent(), properties.getPreviewLength()),
                QuestionTypeEnum.fromId(question.getQuestionType().getId()).name(),
                answered,
                new ListQuestionBankResponse.PracticeExamSummary(
                        question.getPracticeExam().getId(),
                        question.getPracticeExam().getSlug(),
                        question.getPracticeExam().getTitle()),
                question.getDomain() == null ? null : new ListQuestionBankResponse.DomainSummary(
                        question.getDomain().getId(), question.getDomain().getName()));
    }
}

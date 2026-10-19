package br.com.naheroback.modules.questionLists.useCases.addQuestions;

import br.com.naheroback.common.exceptions.custom.UnprocessableEntityException;
import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practiceExams.repositories.QuestionBankRepository;
import br.com.naheroback.modules.practiceExams.services.PracticeAttemptEntitlementService;
import br.com.naheroback.modules.practiceExams.services.QuestionBankProperties;
import br.com.naheroback.modules.practiceExams.services.QuestionBankThrottle;
import br.com.naheroback.modules.questionLists.services.QuestionListService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AddQuestionListQuestionsUseCase {
    private final QuestionListService questionListService;
    private final QuestionBankRepository questionBankRepository;
    private final PracticeAttemptEntitlementService entitlement;
    private final QuestionBankThrottle throttle;
    private final QuestionBankProperties properties;

    @Transactional
    public AddQuestionListQuestionsResponse execute(Integer questionListId, AddQuestionListQuestionsRequest request,
                                                    Locale locale, String captchaToken) {
        entitlement.ensureCanUseQuestionBank();
        Integer studentId = AuthService.getUserFromToken().getId();
        questionListService.loadOwned(questionListId, studentId);

        if (request.filter() != null) {
            throttle.check(studentId, captchaToken);
            return addMatching(questionListId, request.filter(), studentId, locale);
        }

        List<Integer> questionIds = request.questionIds() == null
                ? List.of()
                : request.questionIds().stream().distinct().toList();
        if (questionIds.isEmpty()) {
            throw new UnprocessableEntityException("question_list.empty");
        }
        if (questionIds.size() > properties.getMaxListSize()) {
            throw new UnprocessableEntityException("question_list.full", properties.getMaxListSize());
        }
        questionListService.rejectUnavailable(questionIds);

        return new AddQuestionListQuestionsResponse(questionListService.append(questionListId, questionIds), false);
    }

    private AddQuestionListQuestionsResponse addMatching(Integer questionListId, AddQuestionListQuestionsRequest.Filter filter,
                                                         Integer studentId, Locale locale) {
        String language = locale == null ? Locale.ENGLISH.getLanguage() : locale.getLanguage();
        List<Integer> matching = questionBankRepository.findIds(
                new QuestionBankRepository.Filter(
                        studentId, language, filter.practiceExamId(), filter.domainId(), filter.status()),
                properties.getFilterScanLimit());

        List<Integer> missing = questionListService.missingFrom(questionListId, matching);
        int room = questionListService.remainingRoom(questionListId);
        boolean limitReached = missing.size() > room;
        List<Integer> toAdd = limitReached ? missing.subList(0, room) : missing;

        return new AddQuestionListQuestionsResponse(questionListService.append(questionListId, toAdd), limitReached);
    }
}

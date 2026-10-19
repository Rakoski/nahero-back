package br.com.naheroback.modules.questionLists.useCases.create;

import br.com.naheroback.common.exceptions.custom.UnprocessableEntityException;
import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practiceExams.repositories.QuestionBankRepository;
import br.com.naheroback.modules.practiceExams.services.PracticeAttemptEntitlementService;
import br.com.naheroback.modules.practiceExams.services.QuestionBankProperties;
import br.com.naheroback.modules.practiceExams.services.QuestionBankThrottle;
import br.com.naheroback.modules.questionLists.entities.QuestionList;
import br.com.naheroback.modules.questionLists.repositories.QuestionListRepository;
import br.com.naheroback.modules.questionLists.services.QuestionListService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CreateQuestionListUseCase {
    private final QuestionListRepository questionListRepository;
    private final QuestionListService questionListService;
    private final QuestionBankRepository questionBankRepository;
    private final PracticeAttemptEntitlementService entitlement;
    private final QuestionBankThrottle throttle;
    private final QuestionBankProperties properties;

    @Transactional
    public CreateQuestionListResponse execute(CreateQuestionListRequest request, Locale locale, String captchaToken) {
        entitlement.ensureCanUseQuestionBank();
        Integer studentId = AuthService.getUserFromToken().getId();
        throttle.check(studentId, captchaToken);

        if (questionListRepository.countByStudentId(studentId) >= properties.getMaxLists()) {
            throw new UnprocessableEntityException("question_list.limit_reached", properties.getMaxLists());
        }

        List<Integer> picked = request.questionIds() == null
                ? List.of()
                : request.questionIds().stream().distinct().toList();
        if (picked.size() > properties.getMaxListSize()) {
            throw new UnprocessableEntityException("question_list.full", properties.getMaxListSize());
        }
        questionListService.rejectUnavailable(picked);

        List<Integer> questionIds = new ArrayList<>(picked);
        if (request.filter() != null) {
            questionIds.addAll(sample(request.filter(), studentId, locale, picked, properties.getMaxListSize() - picked.size()));
        }
        if (questionIds.isEmpty()) {
            throw new UnprocessableEntityException("question_list.empty");
        }

        QuestionList questionList = new QuestionList();
        questionList.setStudentId(studentId);
        questionList.setName(request.name().trim());
        questionListRepository.save(questionList);
        questionListService.append(questionList.getId(), questionIds);

        return new CreateQuestionListResponse(questionList.getId(), questionList.getName(), questionIds.size());
    }

    private List<Integer> sample(CreateQuestionListRequest.Filter filter, Integer studentId, Locale locale, List<Integer> picked, int room) {
        int wanted = Math.min(filter.limit() == null ? room : filter.limit(), room);
        if (wanted <= 0) return List.of();

        String language = locale == null ? Locale.ENGLISH.getLanguage() : locale.getLanguage();
        Set<Integer> alreadyPicked = new HashSet<>(picked);
        List<Integer> candidates = new ArrayList<>(questionBankRepository.findIds(
                new QuestionBankRepository.Filter(
                        studentId, language, filter.practiceExamId(), filter.domainId(), filter.status()),
                properties.getFilterScanLimit()));
        candidates.removeIf(alreadyPicked::contains);
        Collections.shuffle(candidates);

        return candidates.subList(0, Math.min(wanted, candidates.size()));
    }
}

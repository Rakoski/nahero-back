package br.com.naheroback.modules.questionLists.useCases.listMine;

import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practiceExams.repositories.QuestionBankRepository;
import br.com.naheroback.modules.practiceExams.services.PracticeAttemptEntitlementService;
import br.com.naheroback.modules.questionLists.entities.QuestionList;
import br.com.naheroback.modules.questionLists.entities.QuestionListItem;
import br.com.naheroback.modules.questionLists.repositories.QuestionListItemRepository;
import br.com.naheroback.modules.questionLists.repositories.QuestionListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListMyQuestionListsUseCase {
    private final QuestionListRepository questionListRepository;
    private final QuestionListItemRepository questionListItemRepository;
    private final QuestionBankRepository questionBankRepository;
    private final PracticeAttemptEntitlementService entitlement;

    @Transactional(readOnly = true)
    public List<ListMyQuestionListsResponse> execute() {
        entitlement.ensureCanUseQuestionBank();
        Integer studentId = AuthService.getUserFromToken().getId();

        List<QuestionList> lists = questionListRepository.findAllByStudentIdOrderByCreatedAtDesc(studentId);
        if (lists.isEmpty()) return List.of();

        List<QuestionListItem> items = questionListItemRepository.findAllByQuestionListIdIn(
                lists.stream().map(QuestionList::getId).toList());
        Set<Integer> visible = questionBankRepository.findVisibleIds(
                items.stream().map(QuestionListItem::getQuestionId).collect(Collectors.toSet()));
        Set<Integer> answered = questionBankRepository.findAnsweredAmong(studentId, visible);

        Map<Integer, List<Integer>> questionIdsByList = items.stream()
                .filter(item -> visible.contains(item.getQuestionId()))
                .collect(Collectors.groupingBy(
                        QuestionListItem::getQuestionListId,
                        Collectors.mapping(QuestionListItem::getQuestionId, Collectors.toList())));

        return lists.stream()
                .map(list -> {
                    List<Integer> questionIds = questionIdsByList.getOrDefault(list.getId(), List.of());
                    return new ListMyQuestionListsResponse(
                            list.getId(),
                            list.getName(),
                            questionIds.size(),
                            (int) questionIds.stream().filter(answered::contains).count(),
                            list.getCreatedAt());
                })
                .toList();
    }
}

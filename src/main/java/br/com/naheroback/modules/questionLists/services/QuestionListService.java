package br.com.naheroback.modules.questionLists.services;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.common.exceptions.custom.UnprocessableEntityException;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.repositories.QuestionBankRepository;
import br.com.naheroback.modules.practiceExams.services.QuestionBankProperties;
import br.com.naheroback.modules.questionLists.entities.QuestionList;
import br.com.naheroback.modules.questionLists.entities.QuestionListItem;
import br.com.naheroback.modules.questionLists.repositories.QuestionListItemRepository;
import br.com.naheroback.modules.questionLists.repositories.QuestionListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionListService {
    private final QuestionListRepository questionListRepository;
    private final QuestionListItemRepository questionListItemRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionBankProperties properties;

    public QuestionList loadOwned(Integer questionListId, Integer studentId) {
        return questionListRepository.findByIdAndStudentId(questionListId, studentId)
                .orElseThrow(() -> NotFoundException.with(QuestionList.class, "id", questionListId));
    }

    public List<Question> visibleQuestions(Integer questionListId) {
        List<Integer> orderedIds = questionListItemRepository.findAllByQuestionListIdOrderByPositionAsc(questionListId)
                .stream()
                .map(QuestionListItem::getQuestionId)
                .toList();
        Map<Integer, Question> byId = questionBankRepository.findVisibleByIds(orderedIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        return orderedIds.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    public void rejectUnavailable(Collection<Integer> questionIds) {
        Set<Integer> visible = questionBankRepository.findVisibleIds(questionIds);
        List<Integer> unavailable = questionIds.stream().filter(id -> !visible.contains(id)).sorted().toList();
        if (!unavailable.isEmpty()) {
            throw new UnprocessableEntityException("question_list.question_unavailable", unavailable);
        }
    }

    public List<Integer> missingFrom(Integer questionListId, List<Integer> questionIds) {
        Set<Integer> present = questionListItemRepository.findAllByQuestionListIdOrderByPositionAsc(questionListId)
                .stream()
                .map(QuestionListItem::getQuestionId)
                .collect(Collectors.toSet());

        return questionIds.stream().distinct().filter(id -> !present.contains(id)).toList();
    }

    public int remainingRoom(Integer questionListId) {
        return Math.max(0, properties.getMaxListSize()
                - questionListItemRepository.findAllByQuestionListIdOrderByPositionAsc(questionListId).size());
    }

    public int append(Integer questionListId, List<Integer> questionIds) {
        List<QuestionListItem> existing = questionListItemRepository.findAllByQuestionListIdOrderByPositionAsc(questionListId);
        Set<Integer> present = existing.stream().map(QuestionListItem::getQuestionId).collect(Collectors.toSet());
        List<Integer> added = questionIds.stream().distinct().filter(id -> !present.contains(id)).toList();

        if (existing.size() + added.size() > properties.getMaxListSize()) {
            throw new UnprocessableEntityException("question_list.full", properties.getMaxListSize());
        }

        int nextPosition = existing.isEmpty() ? 0 : existing.getLast().getPosition() + 1;
        List<QuestionListItem> items = new ArrayList<>();
        for (Integer questionId : added) {
            QuestionListItem item = new QuestionListItem();
            item.setQuestionListId(questionListId);
            item.setQuestionId(questionId);
            item.setPosition(nextPosition++);
            items.add(item);
        }
        questionListItemRepository.saveAll(items);

        return added.size();
    }
}

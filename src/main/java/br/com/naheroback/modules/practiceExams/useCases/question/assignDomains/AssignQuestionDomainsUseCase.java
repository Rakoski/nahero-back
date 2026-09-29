package br.com.naheroback.modules.practiceExams.useCases.question.assignDomains;

import br.com.naheroback.common.exceptions.custom.BadRequestException;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import br.com.naheroback.modules.practiceExams.services.ExamDomains;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignQuestionDomainsUseCase {

    private static final int MAX_REJECTIONS_REPORTED = 50;

    private final QuestionRepository questionRepository;
    private final ExamDomains examDomains;

    @Secured("IS_ADMIN")
    @Transactional
    public AssignQuestionDomainsResponse execute(List<AssignQuestionDomainsRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return new AssignQuestionDomainsResponse(0);
        }

        rejectMalformedEntries(requests);
        rejectDuplicateQuestionIds(requests);

        List<Integer> ids = requests.stream().map(AssignQuestionDomainsRequest::questionId).toList();
        Map<Integer, Question> questionsById = questionRepository.findAllByIdInWithPracticeExam(ids).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        List<Integer> unknownIds = ids.stream()
                .filter(id -> !questionsById.containsKey(id))
                .sorted()
                .toList();
        if (!unknownIds.isEmpty()) {
            throw new BadRequestException(
                    "Nothing was written. Unknown or deleted questionId: " + unknownIds);
        }

        List<String> rejections = new ArrayList<>();
        Map<Question, String> pending = new LinkedHashMap<>();

        for (AssignQuestionDomainsRequest request : requests) {
            Question question = questionsById.get(request.questionId());
            String slug = question.getPracticeExam() != null ? question.getPracticeExam().getSlug() : null;

            if (slug == null) {
                rejections.add(request.questionId() + ": not linked to a practice exam");
                continue;
            }
            if (!examDomains.hasPracticeExam(slug)) {
                rejections.add(request.questionId() + ": no domain map for practice exam '" + slug + "'");
                continue;
            }

            Optional<String> canonical = examDomains.canonicalName(slug, request.domain());
            if (canonical.isEmpty()) {
                rejections.add(request.questionId() + ": '" + request.domain()
                        + "' is not a domain of '" + slug + "'");
                continue;
            }

            pending.put(question, canonical.get());
        }

        if (!rejections.isEmpty()) {
            throw new BadRequestException(summarise(rejections, requests.size()));
        }

        pending.forEach(Question::setDomain);
        questionRepository.saveAll(pending.keySet());

        log.info("Assigned domains to {} question(s)", pending.size());
        return new AssignQuestionDomainsResponse(pending.size());
    }

    private void rejectMalformedEntries(List<AssignQuestionDomainsRequest> requests) {
        List<String> malformed = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            AssignQuestionDomainsRequest request = requests.get(i);
            if (request == null || request.questionId() == null) {
                malformed.add("index " + i + ": missing questionId");
            } else if (request.domain() == null || request.domain().isBlank()) {
                malformed.add(request.questionId() + ": missing domain");
            }
        }
        if (!malformed.isEmpty()) {
            throw new BadRequestException("Nothing was written. " + String.join("; ", malformed));
        }
    }

    private void rejectDuplicateQuestionIds(List<AssignQuestionDomainsRequest> requests) {
        List<Integer> duplicates = requests.stream()
                .collect(Collectors.groupingBy(AssignQuestionDomainsRequest::questionId, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        if (!duplicates.isEmpty()) {
            throw new BadRequestException(
                    "Nothing was written. Duplicate questionId in payload: " + duplicates);
        }
    }

    private String summarise(List<String> rejections, int submitted) {
        String detail = rejections.stream()
                .limit(MAX_REJECTIONS_REPORTED)
                .collect(Collectors.joining("; "));
        String suffix = rejections.size() > MAX_REJECTIONS_REPORTED
                ? " (and " + (rejections.size() - MAX_REJECTIONS_REPORTED) + " more)"
                : "";
        return "Nothing was written. Rejected " + rejections.size() + " of " + submitted
                + " assignments: " + detail + suffix;
    }
}

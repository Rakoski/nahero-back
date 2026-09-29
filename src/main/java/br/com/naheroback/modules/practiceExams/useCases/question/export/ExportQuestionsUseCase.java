package br.com.naheroback.modules.practiceExams.useCases.question.export;

import br.com.naheroback.modules.practiceExams.entities.Alternative;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.repositories.AlternativeRepository;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportQuestionsUseCase {

    private final QuestionRepository questionRepository;
    private final AlternativeRepository alternativeRepository;

    @Secured("IS_ADMIN")
    @Transactional(readOnly = true)
    public List<ExportQuestionsResponse> execute(String practiceExamSlug) {
        List<Question> questions = questionRepository.findAllByPracticeExamSlug(practiceExamSlug);
        if (questions.isEmpty()) {
            return List.of();
        }

        List<Integer> questionIds = questions.stream().map(Question::getId).toList();
        Map<Integer, List<String>> optionsByQuestionId = alternativeRepository
                .findAllActiveByQuestionIdIn(questionIds).stream()
                .collect(Collectors.groupingBy(
                        alternative -> alternative.getQuestion().getId(),
                        Collectors.mapping(Alternative::getContent, Collectors.toList())));

        return questions.stream()
                .map(question -> new ExportQuestionsResponse(
                        question.getId(),
                        question.getPracticeExam().getSlug(),
                        question.getLanguage(),
                        question.getContent(),
                        optionsByQuestionId.getOrDefault(question.getId(), List.of()),
                        question.getDomain()))
                .toList();
    }
}

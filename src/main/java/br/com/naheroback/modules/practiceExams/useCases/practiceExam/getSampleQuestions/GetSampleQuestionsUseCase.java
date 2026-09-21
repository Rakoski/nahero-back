package br.com.naheroback.modules.practiceExams.useCases.practiceExam.getSampleQuestions;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.practiceExams.entities.Alternative;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.repositories.AlternativeRepository;
import br.com.naheroback.modules.practiceExams.repositories.PracticeExamRepository;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class GetSampleQuestionsUseCase {
    private static final int SAMPLE_SIZE = 5;

    private final PracticeExamRepository practiceExamRepository;
    private final QuestionRepository questionRepository;
    private final AlternativeRepository alternativeRepository;

    @Transactional(readOnly = true)
    public List<GetSampleQuestionsResponse> execute(String slug, Locale locale) {
        PracticeExam practiceExam = practiceExamRepository.findBySlug(slug)
                .orElseThrow(() -> NotFoundException.with(PracticeExam.class, "slug", slug));

        String language = locale == null ? Locale.ENGLISH.getLanguage() : locale.getLanguage();

        List<Question> questions = questionRepository.findSampleQuestions(
                practiceExam.getId(),
                language,
                PageRequest.of(0, SAMPLE_SIZE)
        );

        if (questions.isEmpty()) return List.of();

        List<Integer> questionIds = questions.stream().map(Question::getId).toList();

        Map<Integer, List<Alternative>> alternativesByQuestion =
                alternativeRepository.findAllActiveByQuestionIdIn(questionIds).stream()
                        .collect(Collectors.groupingBy(alternative -> alternative.getQuestion().getId()));

        return questions.stream()
                .map(question -> toResponse(question, alternativesByQuestion.getOrDefault(question.getId(), List.of())))
                .toList();
    }

    private GetSampleQuestionsResponse toResponse(Question question, List<Alternative> alternatives) {
        List<GetSampleQuestionsResponse.GetSampleQuestionsAlternative> mappedAlternatives = alternatives.stream()
                .map(alternative -> new GetSampleQuestionsResponse.GetSampleQuestionsAlternative(
                        alternative.getId(),
                        alternative.getContent(),
                        alternative.getIsCorrect()
                ))
                .toList();

        return new GetSampleQuestionsResponse(
                question.getId(),
                question.getContent(),
                question.getImageUrl(),
                question.getExplanation(),
                question.getQuestionType() == null ? null : question.getQuestionType().getName(),
                mappedAlternatives
        );
    }
}

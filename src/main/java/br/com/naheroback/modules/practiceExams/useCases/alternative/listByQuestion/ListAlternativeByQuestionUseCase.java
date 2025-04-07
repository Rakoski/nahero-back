package br.com.naheroback.modules.practiceExams.useCases.alternative.listByQuestion;

import br.com.naheroback.modules.practiceExams.repositories.AlternativeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static br.com.naheroback.common.utils.Constants.MAX_ALTERNATIVES;

@Service
@RequiredArgsConstructor
public class ListAlternativeByQuestionUseCase {
    private final AlternativeRepository alternativeRepository;
    private final ListAlternativeByQuestionResponse response;

    public List<ListAlternativeByQuestionResponse> execute(int questionId) {
        return alternativeRepository.findAllByQuestionId(questionId)
                .stream()
                .limit(MAX_ALTERNATIVES)
                .map(response::toPresentation)
                .toList();
    }
}

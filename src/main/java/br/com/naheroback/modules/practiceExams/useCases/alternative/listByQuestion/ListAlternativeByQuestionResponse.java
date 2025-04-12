package br.com.naheroback.modules.practiceExams.useCases.alternative.listByQuestion;

import br.com.naheroback.modules.practiceExams.entities.Alternative;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ListAlternativeByQuestionResponse {
    public Integer id;
    public String imageUrl;
    public String content;
    // I will not get if isCorrect, what if a student hacks the frontend? I will validate their ids and isCorrect
    // on the backend instead

    @Autowired
    private ModelMapper modelMapper;

    public ListAlternativeByQuestionResponse toPresentation(Alternative alternative) {
        return modelMapper.map(alternative, ListAlternativeByQuestionResponse.class);
    }
}

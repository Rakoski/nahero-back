package br.com.naheroback.modules.practiceExams.useCases.question.listStudent;

import br.com.naheroback.modules.practiceExams.entities.Question;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ListQuestionsByStudentResponse {
    public Integer id;
    public String content;
    public String imageUrl;
    public String explanation;
    public Integer points;
    public ListQuestionsByStudentQuestionType questionType;

    public static class ListQuestionsByStudentQuestionType {
        public Integer id;
        public String name;
    }

    @Autowired
    private ModelMapper modelMapper;

    public ListQuestionsByStudentResponse toPresentation(Question question) {
        return modelMapper.map(question, ListQuestionsByStudentResponse.class);
    }
}

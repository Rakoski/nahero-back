package br.com.naheroback.modules.practiceExams.useCases.question.listStudent;

import br.com.naheroback.modules.practiceExams.entities.Question;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Setter
public class ListQuestionsByStudentResponse {
    public Integer timeLimit;
    public String id;
    public String content;
    public String imageUrl;
    public Integer points;
    public QuestionType questionType;

    public static class QuestionType {
        public Integer id;
        public String name;
    }

    @Autowired
    private ModelMapper modelMapper;

    public ListQuestionsByStudentResponse toPresentation(Question question, Integer timeLimit) {
        ListQuestionsByStudentResponse response = modelMapper.map(question, ListQuestionsByStudentResponse.class);

        response.setTimeLimit(timeLimit);

        return response;
    }
}
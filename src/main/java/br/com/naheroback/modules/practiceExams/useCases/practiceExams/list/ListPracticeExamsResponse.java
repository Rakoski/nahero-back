package br.com.naheroback.modules.practiceExams.useCases.practiceExams.list;

import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ListPracticeExamsResponse {
    public String title;
    public String description;
    public Integer passingScore;
    public Integer timeLimit;
    public Integer difficultyLevel;
    public ListPracticeExamsTeacher teacher;
    public ListPracticeExamsExam exam;

    public static class ListPracticeExamsTeacher {
        public Integer id;
        public String name;
    }

    public static class ListPracticeExamsExam {
        public Integer id;
        public String title;
    }

    @Autowired
    private ModelMapper modelMapper;

    public ListPracticeExamsResponse toPresentation(PracticeExam practiceExam) {
        return modelMapper.map(practiceExam, ListPracticeExamsResponse.class);
    }
}

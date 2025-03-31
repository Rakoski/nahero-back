package br.com.naheroback.modules.practiceExams.useCases.question.listStudent;

import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListQuestionsByStudentUseCase {
    private final QuestionRepository questionRepository;
    private final ListQuestionsByStudentResponse listQuestionsByStudentResponse;

    private static final int MAX_QUESTIONS = 65;

    public List<ListQuestionsByStudentResponse> execute(int practiceExamId) {
        return questionRepository.findAllByPracticeExamId(practiceExamId)
                .stream()
                .limit(MAX_QUESTIONS)
                .map(listQuestionsByStudentResponse::toPresentation)
                .collect(Collectors.toList());
    }
}
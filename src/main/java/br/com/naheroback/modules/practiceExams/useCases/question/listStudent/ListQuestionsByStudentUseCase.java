package br.com.naheroback.modules.practiceExams.useCases.question.listStudent;

import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static br.com.naheroback.common.utils.Constants.*;

@Service
@RequiredArgsConstructor
public class ListQuestionsByStudentUseCase {
    private final QuestionRepository questionRepository;
    private final ListQuestionsByStudentResponse listQuestionsByStudentResponse;

    public List<ListQuestionsByStudentResponse> execute(int practiceExamId) {
        return questionRepository.findAllByPracticeExamId(practiceExamId)
                .stream()
                .limit(MAX_EXAM_QUESTIONS)
                .map(listQuestionsByStudentResponse::toPresentation)
                .toList();
    }
}
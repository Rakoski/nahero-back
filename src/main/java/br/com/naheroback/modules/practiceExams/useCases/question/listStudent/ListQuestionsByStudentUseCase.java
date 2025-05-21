package br.com.naheroback.modules.practiceExams.useCases.question.listStudent;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.common.utils.Constants;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.repositories.PracticeExamRepository;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static br.com.naheroback.common.utils.Constants.*;

@Service
@RequiredArgsConstructor
public class ListQuestionsByStudentUseCase {
    private final QuestionRepository questionRepository;
    private final PracticeExamRepository practiceExamRepository;
    private final ListQuestionsByStudentResponse listQuestionsByStudentResponse;

    public List<ListQuestionsByStudentResponse> execute(int practiceExamId) {
        PracticeExam practiceExam = practiceExamRepository.findById(practiceExamId)
                .orElseThrow(() -> NotFoundException.with(Question.class, "practiceExamId", practiceExamId));

        Integer timeLimit = practiceExam.getTimeLimit();

        List<Question> allQuestions = questionRepository.findAllByPracticeExamId(practiceExamId);
        List<Question> shuffledQuestions = new ArrayList<>(allQuestions);
        Collections.shuffle(shuffledQuestions);

        return shuffledQuestions.stream()
                .limit(practiceExam.getNumberOfQuestions() != null ?  practiceExam.getNumberOfQuestions() : MAX_EXAM_QUESTIONS)
                .map(question -> listQuestionsByStudentResponse.toPresentation(question, timeLimit))
                .toList();
    }
}
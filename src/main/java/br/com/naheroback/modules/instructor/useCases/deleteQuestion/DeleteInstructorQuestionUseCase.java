package br.com.naheroback.modules.instructor.useCases.deleteQuestion;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.instructor.services.InstructorAccess;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteInstructorQuestionUseCase {
    private final InstructorAccess instructorAccess;
    private final QuestionRepository questionRepository;

    @Transactional
    @Secured("IS_TEACHER")
    public void execute(Integer practiceExamId, Integer questionId) {
        instructorAccess.ownedPracticeExam(practiceExamId);

        Question question = questionRepository.findByIdAndPracticeExamId(questionId, practiceExamId)
                .orElseThrow(() -> NotFoundException.with(Question.class, "id", questionId));
        question.setDomain(null);
        questionRepository.saveAndFlush(question);
        questionRepository.delete(question);
    }
}

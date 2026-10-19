package br.com.naheroback.modules.instructor.useCases.updateStatus;

import br.com.naheroback.common.exceptions.custom.UnprocessableEntityException;
import br.com.naheroback.modules.instructor.services.InstructorAccess;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.repositories.PracticeExamRepository;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import br.com.naheroback.modules.practiceExams.services.QuestionShuffleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdatePracticeExamStatusUseCase {
    private final InstructorAccess instructorAccess;
    private final PracticeExamRepository practiceExamRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    @Secured("IS_TEACHER")
    public void execute(Integer practiceExamId, UpdatePracticeExamStatusRequest request) {
        PracticeExam practiceExam = instructorAccess.ownedPracticeExam(practiceExamId);

        int required = practiceExam.getNumberOfQuestions() != null
                ? practiceExam.getNumberOfQuestions()
                : QuestionShuffleService.DEFAULT_MAX_EXAM_QUESTIONS;
        if (request.active() && largestLanguageCount(practiceExamId) < required) {
            throw new UnprocessableEntityException("instructor.practice_exam.not_enough_questions", required);
        }

        practiceExam.setIsActive(request.active());
        practiceExamRepository.save(practiceExam);
    }

    private long largestLanguageCount(Integer practiceExamId) {
        return questionRepository.countByLanguageForPracticeExam(practiceExamId).stream()
                .mapToLong(row -> (Long) row[1])
                .max()
                .orElse(0);
    }
}

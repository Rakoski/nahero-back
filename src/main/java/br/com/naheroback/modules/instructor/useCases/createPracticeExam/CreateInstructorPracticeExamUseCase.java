package br.com.naheroback.modules.instructor.useCases.createPracticeExam;

import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.instructor.services.InstructorAccess;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.repositories.PracticeExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateInstructorPracticeExamUseCase {
    private final InstructorAccess instructorAccess;
    private final PracticeExamRepository practiceExamRepository;

    @Transactional
    @Secured("IS_TEACHER")
    public CreateInstructorPracticeExamResponse execute(CreateInstructorPracticeExamRequest request) {
        PracticeExam practiceExam = new PracticeExam();
        practiceExam.setExam(instructorAccess.ownedExam(request.examId()));
        practiceExam.setTeacher(AuthService.getUserFromToken());
        practiceExam.setTitle(request.title().trim());
        practiceExam.setSlug(practiceExamRepository.uniqueSlug(request.title()));
        practiceExam.setDescription(request.description().trim());
        practiceExam.setPassingScore(request.passingScore());
        practiceExam.setTimeLimit(request.timeLimit());
        practiceExam.setNumberOfQuestions(request.numberOfQuestions());
        practiceExam.setIsActive(false);

        PracticeExam saved = practiceExamRepository.save(practiceExam);
        return new CreateInstructorPracticeExamResponse(saved.getId(), saved.getSlug());
    }
}

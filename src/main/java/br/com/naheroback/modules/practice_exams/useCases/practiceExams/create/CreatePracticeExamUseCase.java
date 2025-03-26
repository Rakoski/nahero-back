package br.com.naheroback.modules.practice_exams.useCases.practiceExams.create;

import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practice_exams.entities.PracticeExam;
import br.com.naheroback.modules.practice_exams.repositories.PracticeExamRepository;
import br.com.naheroback.modules.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreatePracticeExamUseCase {
    private final PracticeExamRepository practiceExamRepository;

    @Transactional
    public void execute(CreatePracticeExamRequest input) {
        User teacher = AuthService.getUserFromToken();
        PracticeExam practiceExam = CreatePracticeExamRequest.toDomain(input, teacher);
        practiceExamRepository.save(practiceExam);
    }
}

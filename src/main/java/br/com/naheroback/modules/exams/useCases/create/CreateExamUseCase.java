package br.com.naheroback.modules.exams.useCases.create;

import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.exams.entities.Exam;
import br.com.naheroback.modules.exams.repositories.ExamRepository;
import br.com.naheroback.modules.user.entities.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateExamUseCase {
    private final ExamRepository examRepository;
    private final CreateExamResponse createExamResponse;

    @Transactional
    public CreateExamResponse execute(CreateExamRequest request) {
        User currentUser = AuthService.getUserFromToken();
        Exam exam = request.toDomain(currentUser);
        Exam savedExam = examRepository.save(exam);
        return createExamResponse.toPresentation(savedExam);
    }
}
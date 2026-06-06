package br.com.naheroback.modules.practiceExams.useCases.practiceExam.create;

import br.com.naheroback.common.utils.StringUtils;
import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.repositories.PracticeExamRepository;
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
        practiceExam.setSlug(generateUniqueSlug(input.title()));
        practiceExamRepository.save(practiceExam);
    }

    private String generateUniqueSlug(String title) {
        String base = StringUtils.createSlug(title);
        String candidate = base;
        int suffix = 2;
        while (practiceExamRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}

package br.com.naheroback.modules.practiceExams.services;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentAttemptAccessService {
    private final StudentPracticeAttemptRepository studentPracticeAttemptRepository;

    public StudentPracticeAttempt loadOwnedAttempt(Integer attemptId) {
        return ownedOrNotFound(studentPracticeAttemptRepository.findById(attemptId), attemptId);
    }

    public StudentPracticeAttempt loadOwnedAttemptForUpdate(Integer attemptId) {
        return ownedOrNotFound(studentPracticeAttemptRepository.findByIdForUpdate(attemptId), attemptId);
    }

    private StudentPracticeAttempt ownedOrNotFound(Optional<StudentPracticeAttempt> found, Integer attemptId) {
        StudentPracticeAttempt attempt = found
                .orElseThrow(() -> NotFoundException.with(StudentPracticeAttempt.class, "id", attemptId));

        Integer studentId = AuthService.getUserFromToken().getId();

        if (!Objects.equals(attempt.getEnrollment().getStudent().getId(), studentId)) {
            throw NotFoundException.with(StudentPracticeAttempt.class, "id", attemptId);
        }

        return attempt;
    }
}

package br.com.naheroback.modules.enrollment.useCases.enrollment.create;

import br.com.naheroback.modules.enrollment.entities.Enrollment;
import br.com.naheroback.modules.enrollment.repositories.EnrollmentRepository;
import br.com.naheroback.modules.enrollment.repositories.EnrollmentStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateEnrollmentUseCase {
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public Integer execute(CreateEnrollmentRequest request) {
        Enrollment enrollment = CreateEnrollmentRequest.toDomain(request);
        enrollmentRepository.save(enrollment);
        return enrollment.getId();
    }
}

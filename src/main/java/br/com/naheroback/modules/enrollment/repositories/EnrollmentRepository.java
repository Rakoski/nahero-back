package br.com.naheroback.modules.enrollment.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.enrollment.entities.Enrollment;

import java.util.Optional;

public interface EnrollmentRepository extends BaseRepository<Enrollment, Integer> {
    Optional<Enrollment> findByExamIdAndStudentId(Integer examId, Integer studentId);
}

package br.com.naheroback.modules.enrollment.useCases.enrollment.create;

import br.com.naheroback.common.utils.ValidateNull;
import br.com.naheroback.modules.enrollment.entities.Enrollment;
import br.com.naheroback.modules.enrollment.entities.EnrollmentStatus;
import br.com.naheroback.modules.enrollment.entities.enums.EnrollmentStatusEnum;
import br.com.naheroback.modules.exams.entities.Exam;
import br.com.naheroback.modules.user.entities.User;
import jakarta.validation.constraints.NotNull;

public record CreateEnrollmentRequest(
    @NotNull(message = "studentId is required") Integer studentId,
    @NotNull(message = "examId is required") Integer examId
) {
    public static Enrollment toDomain(CreateEnrollmentRequest request) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(ValidateNull.validate(User.class, request.studentId));
        enrollment.setExam(ValidateNull.validate(Exam.class, request.examId));
        enrollment.setStatus(ValidateNull.validate(EnrollmentStatus.class, EnrollmentStatusEnum.ACTIVE.getId()));
        return enrollment;
    }
}

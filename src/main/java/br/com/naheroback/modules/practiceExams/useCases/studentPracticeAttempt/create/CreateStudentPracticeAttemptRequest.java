package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.create;

import br.com.naheroback.common.utils.ValidateNull;
import br.com.naheroback.modules.enrollment.entities.Enrollment;
import br.com.naheroback.modules.practiceExams.entities.PracticeAttemptStatus;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.entities.enums.PracticeAttemptStatusesEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record CreateStudentPracticeAttemptRequest(
    @NotNull(message = "practiceExamId is required") @Positive Integer practiceExamId
) {
    public static StudentPracticeAttempt toDomain(Integer enrollmentId, Integer practiceExamId) {
        StudentPracticeAttempt studentPracticeAttempt = new StudentPracticeAttempt();
        studentPracticeAttempt.setEnrollment(ValidateNull.validate(Enrollment.class, enrollmentId));
        studentPracticeAttempt.setPracticeExam(ValidateNull.validate(PracticeExam.class, practiceExamId));
        studentPracticeAttempt.setAttemptStatus
                (ValidateNull.validate(PracticeAttemptStatus.class, PracticeAttemptStatusesEnum.IN_PROGRESS.getId()));
        studentPracticeAttempt.setStartTime(LocalDateTime.now());
        return studentPracticeAttempt;
    }
}

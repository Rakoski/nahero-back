package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.create;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.enrollment.entities.Enrollment;
import br.com.naheroback.modules.enrollment.repositories.EnrollmentRepository;
import br.com.naheroback.modules.enrollment.useCases.enrollment.create.CreateEnrollmentRequest;
import br.com.naheroback.modules.enrollment.useCases.enrollment.create.CreateEnrollmentUseCase;
import br.com.naheroback.modules.exams.entities.Exam;
import br.com.naheroback.modules.exams.repositories.ExamRepository;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.repositories.PracticeExamRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreateStudentPracticeAttemptUseCase {
    private final StudentPracticeAttemptRepository studentPracticeAttemptRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CreateEnrollmentUseCase createEnrollmentUseCase;
    private final PracticeExamRepository practiceExamRepository;
    private final ExamRepository examRepository;

    @Transactional
    @Secured("IS_STUDENT")
    public Integer execute(CreateStudentPracticeAttemptRequest request) {
        int practiceExamId = request.practiceExamId();
        PracticeExam practiceExam = practiceExamRepository.findById(practiceExamId)
            .orElseThrow(() -> NotFoundException.with(PracticeExam.class, "id", practiceExamId));

        int examId = practiceExam.getExam().getId();
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> NotFoundException.with(Exam.class, "id", examId));

        Integer studentId = AuthService.getUserFromToken().getId();
        Optional<Enrollment> studentsEnrollment = enrollmentRepository.findByExamIdAndStudentId(exam.getId(), studentId);
        Integer enrollmentId;

        if (studentsEnrollment.isEmpty()) {
            CreateEnrollmentRequest createEnrollmentRequest = new CreateEnrollmentRequest(studentId, exam.getId());
            enrollmentId = createEnrollmentUseCase.execute(createEnrollmentRequest);
        } else {
            enrollmentId = studentsEnrollment.get().getId();
        }

        StudentPracticeAttempt studentPracticeAttempt = CreateStudentPracticeAttemptRequest.toDomain(enrollmentId, practiceExamId);
        studentPracticeAttemptRepository.save(studentPracticeAttempt);
        return studentPracticeAttempt.getId();
    }
}

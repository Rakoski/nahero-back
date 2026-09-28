package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.create;

import br.com.naheroback.common.exceptions.custom.ConflictException;
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
import br.com.naheroback.modules.practiceExams.entities.enums.PracticeAttemptStatusesEnum;
import br.com.naheroback.modules.practiceExams.repositories.PracticeExamRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.abandon.AbandonStudentPracticeAttemptUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateStudentPracticeAttemptUseCase {
    public static final String IN_PROGRESS_CONFLICT_CODE = "ATTEMPT_IN_PROGRESS";

    private final StudentPracticeAttemptRepository studentPracticeAttemptRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CreateEnrollmentUseCase createEnrollmentUseCase;
    private final AbandonStudentPracticeAttemptUseCase abandonStudentPracticeAttemptUseCase;
    private final PracticeExamRepository practiceExamRepository;
    private final ExamRepository examRepository;

    @Transactional
    @Secured("IS_STUDENT")
    public Integer execute(CreateStudentPracticeAttemptRequest request, Locale locale) {
        int practiceExamId = request.practiceExamId();
        PracticeExam practiceExam = practiceExamRepository.findById(practiceExamId)
            .orElseThrow(() -> NotFoundException.with(PracticeExam.class, "id", practiceExamId));

        int examId = practiceExam.getExam().getId();
        Exam exam = examRepository.findById(examId).orElseThrow(() -> NotFoundException.with(Exam.class, "id", examId));

        Integer studentId = AuthService.getUserFromToken().getId();

        List<StudentPracticeAttempt> inProgress = studentPracticeAttemptRepository.findByStudentAndStatus(
                studentId, PracticeAttemptStatusesEnum.IN_PROGRESS.getId());

        Optional<StudentPracticeAttempt> resumable = inProgress.stream()
                .filter(attempt -> Objects.equals(attempt.getPracticeExam().getId(), practiceExamId))
                .findFirst();

        if (resumable.isPresent()) return resumable.get().getId();

        if (!inProgress.isEmpty()) {
            StudentPracticeAttempt current = inProgress.getFirst();

            if (!Boolean.TRUE.equals(request.discardInProgress())) {
                throw new ConflictException("attempt.in_progress_conflict", IN_PROGRESS_CONFLICT_CODE, current.getPracticeExam().getTitle());
            }

            inProgress.forEach(attempt -> abandonStudentPracticeAttemptUseCase.execute(attempt.getId()));
        }

        Optional<Enrollment> studentsEnrollment = enrollmentRepository.findByExamIdAndStudentId(exam.getId(), studentId);
        Integer enrollmentId;

        if (studentsEnrollment.isEmpty()) {
            CreateEnrollmentRequest createEnrollmentRequest = new CreateEnrollmentRequest(studentId, exam.getId());
            enrollmentId = createEnrollmentUseCase.execute(createEnrollmentRequest);
        } else {
            enrollmentId = studentsEnrollment.get().getId();
        }

        StudentPracticeAttempt studentPracticeAttempt = CreateStudentPracticeAttemptRequest.toDomain(enrollmentId, practiceExamId);
        studentPracticeAttempt.setLanguage(locale.getLanguage());
        studentPracticeAttemptRepository.save(studentPracticeAttempt);
        return studentPracticeAttempt.getId();
    }
}

package br.com.naheroback.modules.instructor.services;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.exams.entities.Exam;
import br.com.naheroback.modules.exams.repositories.ExamRepository;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.repositories.PracticeExamRepository;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.entities.enums.RolesEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InstructorAccess {
    private final ExamRepository examRepository;
    private final PracticeExamRepository practiceExamRepository;

    public Exam ownedExam(Integer examId) {
        User user = AuthService.getUserFromToken();
        return examRepository.findById(examId)
                .filter(exam -> canManage(user, exam.getTeacher()))
                .orElseThrow(() -> NotFoundException.with(Exam.class, "id", examId));
    }

    public PracticeExam ownedPracticeExam(Integer practiceExamId) {
        User user = AuthService.getUserFromToken();
        return practiceExamRepository.findById(practiceExamId)
                .filter(practiceExam -> canManage(user, practiceExam.getTeacher()))
                .orElseThrow(() -> NotFoundException.with(PracticeExam.class, "id", practiceExamId));
    }

    private boolean canManage(User user, User owner) {
        if (user.hasRole(RolesEnum.IS_ADMIN.name())) return true;
        return owner != null && Objects.equals(owner.getId(), user.getId());
    }
}

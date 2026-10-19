package br.com.naheroback.modules.instructor.useCases.listExams;

import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.exams.entities.Exam;
import br.com.naheroback.modules.exams.repositories.ExamRepository;
import br.com.naheroback.modules.practiceExams.repositories.PracticeExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListInstructorExamsUseCase {
    private final ExamRepository examRepository;
    private final PracticeExamRepository practiceExamRepository;

    @Transactional(readOnly = true)
    @Secured("IS_TEACHER")
    public List<ListInstructorExamsResponse> execute() {
        List<Exam> exams = examRepository.findAllByTeacherIdOrderByCreatedAtDesc(AuthService.getUserFromToken().getId());
        if (exams.isEmpty()) return List.of();

        Map<Integer, Long> practiceExamCounts = practiceExamRepository
                .countByExamIds(exams.stream().map(Exam::getId).toList()).stream()
                .collect(Collectors.toMap(row -> (Integer) row[0], row -> (Long) row[1]));

        return exams.stream()
                .map(exam -> ListInstructorExamsResponse.from(exam, practiceExamCounts.getOrDefault(exam.getId(), 0L)))
                .toList();
    }
}

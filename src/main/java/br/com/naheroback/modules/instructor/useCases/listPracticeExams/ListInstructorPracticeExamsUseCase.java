package br.com.naheroback.modules.instructor.useCases.listPracticeExams;

import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.repositories.PracticeExamRepository;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListInstructorPracticeExamsUseCase {
    private final PracticeExamRepository practiceExamRepository;
    private final QuestionRepository questionRepository;

    @Transactional(readOnly = true)
    @Secured("IS_TEACHER")
    public List<ListInstructorPracticeExamsResponse> execute() {
        List<PracticeExam> practiceExams = practiceExamRepository
                .findAllByTeacherIdOrderByCreatedAtDesc(AuthService.getUserFromToken().getId());
        if (practiceExams.isEmpty()) return List.of();

        Map<Integer, Long> questionCounts = questionRepository
                .countByPracticeExamIds(practiceExams.stream().map(PracticeExam::getId).toList()).stream()
                .collect(Collectors.toMap(row -> (Integer) row[0], row -> (Long) row[1]));

        return practiceExams.stream()
                .map(practiceExam -> ListInstructorPracticeExamsResponse.from(
                        practiceExam, questionCounts.getOrDefault(practiceExam.getId(), 0L)))
                .toList();
    }
}

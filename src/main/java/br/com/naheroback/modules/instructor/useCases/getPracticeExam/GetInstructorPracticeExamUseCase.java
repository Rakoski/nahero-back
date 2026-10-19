package br.com.naheroback.modules.instructor.useCases.getPracticeExam;

import br.com.naheroback.modules.exams.entities.Exam;
import br.com.naheroback.modules.instructor.services.InstructorAccess;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.repositories.PracticeExamDomainRepository;
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
public class GetInstructorPracticeExamUseCase {
    private final InstructorAccess instructorAccess;
    private final PracticeExamDomainRepository practiceExamDomainRepository;
    private final QuestionRepository questionRepository;

    @Transactional(readOnly = true)
    @Secured("IS_TEACHER")
    public GetInstructorPracticeExamResponse execute(Integer practiceExamId) {
        PracticeExam practiceExam = instructorAccess.ownedPracticeExam(practiceExamId);

        Map<Integer, Long> questionsByDomain = questionRepository.countByDomainForPracticeExam(practiceExamId).stream()
                .collect(Collectors.toMap(row -> (Integer) row[0], row -> (Long) row[1]));
        List<GetInstructorPracticeExamResponse.DomainSummary> domains = practiceExamDomainRepository
                .findAllByPracticeExamIdIn(List.of(practiceExamId)).stream()
                .map(link -> new GetInstructorPracticeExamResponse.DomainSummary(
                        link.getDomain().getId(),
                        link.getDomain().getName(),
                        link.getMinWeight(),
                        link.getMaxWeight(),
                        questionsByDomain.getOrDefault(link.getDomain().getId(), 0L)))
                .toList();

        Exam exam = practiceExam.getExam();
        return new GetInstructorPracticeExamResponse(
                practiceExam.getId(),
                practiceExam.getSlug(),
                practiceExam.getTitle(),
                practiceExam.getDescription(),
                practiceExam.getPassingScore(),
                practiceExam.getTimeLimit(),
                practiceExam.getNumberOfQuestions(),
                practiceExam.getIsActive(),
                questionRepository.countAllByPracticeExamId(practiceExamId),
                questionRepository.countByLanguageForPracticeExam(practiceExamId).stream()
                        .filter(row -> row[0] != null)
                        .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1])),
                exam == null ? null : new GetInstructorPracticeExamResponse.ExamSummary(
                        exam.getId(), exam.getTitle(), exam.getCategory(), exam.getDifficultyLevel()),
                domains);
    }
}

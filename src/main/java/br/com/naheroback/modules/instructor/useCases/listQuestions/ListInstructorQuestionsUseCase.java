package br.com.naheroback.modules.instructor.useCases.listQuestions;

import br.com.naheroback.common.utils.StringUtils;
import br.com.naheroback.modules.instructor.services.InstructorAccess;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.entities.enums.QuestionTypeEnum;
import br.com.naheroback.modules.practiceExams.repositories.AlternativeRepository;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListInstructorQuestionsUseCase {
    private static final int MAX_PAGE_SIZE = 50;
    private static final int PREVIEW_LENGTH = 200;

    private final InstructorAccess instructorAccess;
    private final QuestionRepository questionRepository;
    private final AlternativeRepository alternativeRepository;

    @Transactional(readOnly = true)
    @Secured("IS_TEACHER")
    public Page<ListInstructorQuestionsResponse> execute(Integer practiceExamId, int page, int size) {
        instructorAccess.ownedPracticeExam(practiceExamId);

        Page<Question> questions = questionRepository.findByPracticeExamIdOrderByIdDesc(
                practiceExamId, PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE)));
        Map<Integer, Object[]> alternativeCounts = questions.isEmpty()
                ? Map.of()
                : alternativeRepository.countByQuestionIds(questions.map(Question::getId).getContent()).stream()
                        .collect(Collectors.toMap(row -> (Integer) row[0], row -> row));

        return questions.map(question -> {
            Object[] counts = alternativeCounts.get(question.getId());
            return new ListInstructorQuestionsResponse(
                    question.getId(),
                    StringUtils.plainText(question.getContent(), PREVIEW_LENGTH),
                    QuestionTypeEnum.fromId(question.getQuestionType() == null ? null : question.getQuestionType().getId()).name(),
                    question.getLanguage(),
                    question.getDomain() == null
                            ? null
                            : new ListInstructorQuestionsResponse.DomainSummary(question.getDomain().getId(), question.getDomain().getName()),
                    counts == null ? 0 : ((Number) counts[1]).longValue(),
                    counts == null ? 0 : ((Number) counts[2]).longValue(),
                    question.getCreatedAt());
        });
    }
}

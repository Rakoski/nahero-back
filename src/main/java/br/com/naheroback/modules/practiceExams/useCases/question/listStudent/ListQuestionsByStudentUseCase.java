package br.com.naheroback.modules.practiceExams.useCases.question.listStudent;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import br.com.naheroback.modules.practiceExams.services.QuestionShuffleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListQuestionsByStudentUseCase {
    private final QuestionRepository questionRepository;
    private final StudentPracticeAttemptRepository studentPracticeAttemptRepository;
    private final ListQuestionsByStudentResponse listQuestionsByStudentResponse;
    private final QuestionShuffleService questionShuffleService;

    private static final int MAX_EXAM_QUESTIONS = 70;

    public Page<ListQuestionsByStudentResponse> execute(int attemptId, Pageable pageable, String language) {
        StudentPracticeAttempt attempt = studentPracticeAttemptRepository.findById(attemptId)
                .orElseThrow(() -> NotFoundException.with(StudentPracticeAttempt.class, "attemptId", attemptId));

        PracticeExam practiceExam = attempt.getPracticeExam();
        Integer timeLimit = practiceExam.getTimeLimit();

        Integer dbCount = questionRepository.countAllByPracticeExamId(practiceExam.getId());
        int effectiveTotal = Math.min(dbCount, MAX_EXAM_QUESTIONS);

        List<Integer> shuffledIdsForPage = questionShuffleService.getShuffledQuestionIdsForPage(
                attemptId,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                language
        );

        if (shuffledIdsForPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, effectiveTotal);
        }

        List<Question> questions = questionRepository.findAllByIdIn(shuffledIdsForPage);

        Map<Integer, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        List<ListQuestionsByStudentResponse> responses = shuffledIdsForPage.stream()
                .map(questionMap::get)
                .filter(Objects::nonNull)
                .map(question -> listQuestionsByStudentResponse.toPresentation(question, timeLimit))
                .toList();

        return new PageImpl<>(responses, pageable, effectiveTotal);
    }
}
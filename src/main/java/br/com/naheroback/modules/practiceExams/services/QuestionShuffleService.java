package br.com.naheroback.modules.practiceExams.services;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.common.exceptions.custom.UnprocessableEntityException;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionShuffleService {
    
    private final QuestionRepository questionRepository;
    private final StudentPracticeAttemptRepository studentPracticeAttemptRepository;
    
    public static final int DEFAULT_MAX_EXAM_QUESTIONS = 70;

    public List<Integer> getShuffledQuestionIds(Integer attemptId, String language) {
        StudentPracticeAttempt attempt = studentPracticeAttemptRepository.findById(attemptId)
                .orElseThrow(() -> NotFoundException.with(StudentPracticeAttempt.class, "attemptId", attemptId));
        
        if (attempt.getShuffledQuestionIds() != null && !attempt.getShuffledQuestionIds().isEmpty()) {
            return attempt.getShuffledQuestionIds();
        }

        Integer practiceExamId = attempt.getPracticeExam().getId();
        int maxQuestions = getMaxQuestionsForExam(attempt);

        List<Integer> questionIds = new ArrayList<>(
                questionRepository.findAllIdsByPracticeExamIdAndLanguage(practiceExamId, language)
        );

        if (questionIds.size() < maxQuestions) {
            throw new UnprocessableEntityException(
                    "Practice exam %d does not have enough questions in language '%s' (required: %d, available: %d)"
                            .formatted(practiceExamId, language, maxQuestions, questionIds.size())
            );
        }

        Collections.shuffle(questionIds);

        List<Integer> cappedIds = questionIds.stream()
                .limit(maxQuestions)
                .toList();

        attempt.setShuffledQuestionIds(cappedIds);
        studentPracticeAttemptRepository.save(attempt);
        
        return cappedIds;
    }

    public int getMaxQuestionsForExam(StudentPracticeAttempt attempt) {
        Integer numberOfQuestions = attempt.getPracticeExam().getNumberOfQuestions();
        return numberOfQuestions != null ? numberOfQuestions : DEFAULT_MAX_EXAM_QUESTIONS;
    }

    public List<Integer> getShuffledQuestionIdsForPage(Integer attemptId, int page, int size, String language) {
        List<Integer> allShuffledIds = getShuffledQuestionIds(attemptId, language);

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, allShuffledIds.size());
        if (fromIndex >= allShuffledIds.size()) return new ArrayList<>();

        return allShuffledIds.subList(fromIndex, toIndex);
    }
}

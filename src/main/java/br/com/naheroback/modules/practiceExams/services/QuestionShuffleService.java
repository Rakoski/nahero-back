package br.com.naheroback.modules.practiceExams.services;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
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
    
    public List<Integer> getShuffledQuestionIds(Integer attemptId) {
        StudentPracticeAttempt attempt = studentPracticeAttemptRepository.findById(attemptId)
                .orElseThrow(() -> NotFoundException.with(StudentPracticeAttempt.class, "attemptId", attemptId));
        
        if (attempt.getShuffledQuestionIds() != null && !attempt.getShuffledQuestionIds().isEmpty()) {
            return attempt.getShuffledQuestionIds();
        }

        Integer practiceExamId = attempt.getPracticeExam().getId();
        List<Integer> questionIds = new ArrayList<>(
                questionRepository.findAllIdsByPracticeExamId(practiceExamId)
        );

        Collections.shuffle(questionIds);

        attempt.setShuffledQuestionIds(questionIds);
        studentPracticeAttemptRepository.save(attempt);
        
        return questionIds;
    }

    public List<Integer> getShuffledQuestionIdsForPage(Integer attemptId, int page, int size) {
        List<Integer> allShuffledIds = getShuffledQuestionIds(attemptId);

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, allShuffledIds.size());
        if (fromIndex >= allShuffledIds.size()) return new ArrayList<>();

        return allShuffledIds.subList(fromIndex, toIndex);
    }
}


package br.com.naheroback.modules.practiceExams.services;

import br.com.naheroback.modules.practiceExams.entities.StudentAttemptAnswerDraft;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.repositories.StudentAttemptAnswerDraftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttemptAnswerDraftService {
    private final StudentAttemptAnswerDraftRepository draftRepository;

    public void saveAnswer(StudentPracticeAttempt attempt, Integer questionId, List<Integer> alternativeIds) {
        Optional<StudentAttemptAnswerDraft> existing = draftRepository.findByStudentPracticeAttemptIdAndQuestionId(attempt.getId(), questionId);

        if (alternativeIds == null || alternativeIds.isEmpty()) {
            existing.ifPresent(draftRepository::delete);
            return;
        }

        StudentAttemptAnswerDraft draft = existing.orElseGet(() -> {
            StudentAttemptAnswerDraft created = new StudentAttemptAnswerDraft();
            created.setStudentPracticeAttempt(attempt);
            created.setQuestionId(questionId);
            return created;
        });

        draft.setSelectedAlternativeIds(alternativeIds);
        draftRepository.save(draft);
    }

    public List<StudentAttemptAnswerDraft> findAllByAttempt(Integer attemptId) {
        return draftRepository.findAllByStudentPracticeAttemptId(attemptId);
    }

    public List<AttemptScoringService.AnswerData> mergeWithSubmitted(Integer attemptId, List<AttemptScoringService.AnswerData> submitted) {
        Map<Integer, AttemptScoringService.AnswerData> merged = new LinkedHashMap<>();

        for (StudentAttemptAnswerDraft draft : findAllByAttempt(attemptId)) {
            merged.put(draft.getQuestionId(), new AttemptScoringService.AnswerData(
                    String.valueOf(draft.getQuestionId()),
                    draft.getSelectedAlternativeIds().stream().map(String::valueOf).toList(),
                    null,
                    null
            ));
        }

        if (submitted != null) {
            for (AttemptScoringService.AnswerData answer : submitted) {
                merged.put(Integer.parseInt(answer.questionId()), answer);
            }
        }

        return List.copyOf(merged.values());
    }
}

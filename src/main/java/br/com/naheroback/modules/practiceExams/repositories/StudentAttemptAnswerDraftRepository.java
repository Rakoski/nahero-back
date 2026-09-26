package br.com.naheroback.modules.practiceExams.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.practiceExams.entities.StudentAttemptAnswerDraft;

import java.util.List;
import java.util.Optional;

public interface StudentAttemptAnswerDraftRepository extends BaseRepository<StudentAttemptAnswerDraft, Integer> {

    List<StudentAttemptAnswerDraft> findAllByStudentPracticeAttemptId(Integer studentPracticeAttemptId);

    Optional<StudentAttemptAnswerDraft> findByStudentPracticeAttemptIdAndQuestionId(Integer studentPracticeAttemptId,
                                                                                   Integer questionId);
}

package br.com.naheroback.modules.practiceExams.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.practiceExams.entities.StudentAnswer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentAnswerRepository extends BaseRepository<StudentAnswer, Integer> {

    Page<StudentAnswer> findByStudentPracticeAttemptId(Integer studentPracticeAttemptId, Pageable pageable);

    @Query("""
        SELECT sa FROM StudentAnswer sa
        JOIN Question q ON sa.questionId = q.id AND sa.questionVersion = q.version
        WHERE sa.studentPracticeAttempt.id = :attemptId
        AND (:isCorrect IS NULL OR sa.isCorrect = :isCorrect)
        AND (
            CAST(:questionContent AS string) IS NULL
            OR LOWER(q.content) LIKE LOWER(CONCAT('%', CAST(:questionContent AS string), '%'))
        )
    """)
    Page<StudentAnswer> findByAttemptIdWithFilters(
            @Param("attemptId") Integer attemptId,
            @Param("isCorrect") Boolean isCorrect,
            @Param("questionContent") String questionContent,
            Pageable pageable
    );
}

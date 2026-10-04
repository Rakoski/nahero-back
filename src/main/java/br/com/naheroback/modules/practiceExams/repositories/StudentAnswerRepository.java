package br.com.naheroback.modules.practiceExams.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.practiceExams.entities.StudentAnswer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface StudentAnswerRepository extends BaseRepository<StudentAnswer, Integer> {

    List<StudentAnswer> findAllByStudentPracticeAttemptId(Integer studentPracticeAttemptId);

    List<StudentAnswer> findAllByStudentPracticeAttemptIdIn(Collection<Integer> studentPracticeAttemptIds);

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
        AND sa.id IN (
            SELECT MIN(sa2.id)
            FROM StudentAnswer sa2
            WHERE sa2.studentPracticeAttempt.id = :attemptId\s
            GROUP BY sa2.questionId
        )
    """)
    Page<StudentAnswer> findByAttemptIdWithFilters(
            @Param("attemptId") Integer attemptId,
            @Param("isCorrect") Boolean isCorrect,
            @Param("questionContent") String questionContent,
            Pageable pageable
    );

    @Query("""
        SELECT a.endTime AS endTime,
               COUNT(DISTINCT CASE WHEN sa.selectedAlternativeId IS NOT NULL THEN sa.questionId END) AS answered,
               COUNT(DISTINCT CASE WHEN sa.isCorrect = true THEN sa.questionId END) AS correct
        FROM StudentAnswer sa
        JOIN sa.studentPracticeAttempt a
        WHERE a.enrollment.student.id = :studentId AND a.endTime IS NOT NULL
        GROUP BY a.id, a.endTime
    """)
    List<AttemptQuestionCount> countQuestionsPerAttempt(@Param("studentId") Integer studentId);
}

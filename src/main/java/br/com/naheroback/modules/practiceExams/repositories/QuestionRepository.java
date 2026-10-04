package br.com.naheroback.modules.practiceExams.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.practiceExams.entities.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends BaseRepository<Question, Integer> {
    int findVersionByBaseQuestionId(int baseQuestionId);
    List<Question> findAllByPracticeExamId(int practiceExamId);
    Integer countAllByPracticeExamId(int practiceExamId);
    Page<Question> findAllByPracticeExamId(Integer practiceExamId, Pageable pageable);

    @Query("SELECT q FROM Question q WHERE q.id IN :ids")
    List<Question> findAllByIdIn(@Param("ids") List<Integer> ids);

    @Query("SELECT q FROM Question q JOIN FETCH q.practiceExam WHERE q.id IN :ids")
    List<Question> findAllByIdInWithPracticeExam(@Param("ids") List<Integer> ids);

    @Query("""
            SELECT q FROM Question q
            JOIN FETCH q.practiceExam pe
            WHERE pe.slug = :slug
            ORDER BY q.id ASC
            """)
    List<Question> findAllByPracticeExamSlug(@Param("slug") String slug);

    @Query("SELECT q.id FROM Question q WHERE q.practiceExam.id = :practiceExamId")
    List<Integer> findAllIdsByPracticeExamId(@Param("practiceExamId") Integer practiceExamId);

    @Query("SELECT q.id FROM Question q WHERE q.practiceExam.id = :practiceExamId AND q.language = :language")
    List<Integer> findAllIdsByPracticeExamIdAndLanguage(@Param("practiceExamId") Integer practiceExamId, @Param("language") String language);

    @Query(value = """
            SELECT q.id FROM questions q
            WHERE q.practice_exam_id = :practiceExamId
              AND q.domain = :domain
              AND q.language = :language
              AND q.deleted_at IS NULL
              AND COALESCE(q.is_active, true) = true
              AND q.question_type_id IN (1, 2, 3)
              AND NOT EXISTS (
                  SELECT 1 FROM student_answers sa
                  JOIN student_practice_attempts a ON a.id = sa.student_practice_attempt_id AND a.deleted_at IS NULL
                  JOIN enrollments e ON e.id = a.enrollment_id
                  WHERE e.student_id = :studentId AND sa.question_id = q.id
                    AND sa.is_correct = true AND sa.deleted_at IS NULL)
            ORDER BY EXISTS (
                  SELECT 1 FROM student_answers sa
                  JOIN student_practice_attempts a ON a.id = sa.student_practice_attempt_id AND a.deleted_at IS NULL
                  JOIN enrollments e ON e.id = a.enrollment_id
                  WHERE e.student_id = :studentId AND sa.question_id = q.id AND sa.deleted_at IS NULL),
                random()
            LIMIT :limit
            """, nativeQuery = true)
    List<Integer> findPracticeQuestionIds(@Param("practiceExamId") Integer practiceExamId,
                                          @Param("domain") String domain,
                                          @Param("language") String language,
                                          @Param("studentId") Integer studentId,
                                          @Param("limit") int limit);

    @Query("""
            SELECT q FROM Question q
            WHERE q.practiceExam.id = :practiceExamId
              AND q.language = :language
              AND COALESCE(q.isActive, true) = true
              AND q.explanation IS NOT NULL
              AND LENGTH(TRIM(q.explanation)) > 0
            ORDER BY q.id ASC
            """)
    List<Question> findSampleQuestions(@Param("practiceExamId") Integer practiceExamId,
                                       @Param("language") String language,
                                       Pageable pageable);
}

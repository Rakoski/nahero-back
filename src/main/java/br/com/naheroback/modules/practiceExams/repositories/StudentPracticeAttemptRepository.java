package br.com.naheroback.modules.practiceExams.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StudentPracticeAttemptRepository extends BaseRepository<StudentPracticeAttempt, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM StudentPracticeAttempt a WHERE a.id = :id")
    Optional<StudentPracticeAttempt> findByIdForUpdate(@Param("id") Integer id);

    @Query("""
        SELECT a FROM StudentPracticeAttempt a
        JOIN FETCH a.practiceExam
        JOIN FETCH a.attemptStatus
        JOIN FETCH a.enrollment e
        JOIN FETCH e.student
        WHERE a.id = :id
    """)
    Optional<StudentPracticeAttempt> findByIdWithDetails(@Param("id") Integer id);

    @Query("""
        SELECT a FROM StudentPracticeAttempt a
        LEFT JOIN FETCH a.practiceExam
        LEFT JOIN FETCH a.attemptStatus
        WHERE a.enrollment.student.id = :studentId
    """)
    List<StudentPracticeAttempt> findAllForStudentDashboard(@Param("studentId") Integer studentId);

    @Query("""
        SELECT a FROM StudentPracticeAttempt a
        LEFT JOIN FETCH a.practiceExam
        LEFT JOIN FETCH a.attemptStatus
        WHERE a.enrollment.student.id = :studentId AND a.attemptStatus.id = :statusId
        ORDER BY a.startTime DESC, a.id DESC
    """)
    List<StudentPracticeAttempt> findByStudentAndStatus(@Param("studentId") Integer studentId,
                                                       @Param("statusId") Integer statusId);

    @Query("""
        SELECT a FROM StudentPracticeAttempt a
        JOIN FETCH a.practiceExam
        JOIN FETCH a.attemptStatus
        WHERE a.enrollment.student.id = :studentId
        ORDER BY a.startTime DESC, a.id DESC
    """)
    List<StudentPracticeAttempt> findLatestForStudent(@Param("studentId") Integer studentId, Pageable pageable);

    @Query("""
        SELECT a FROM StudentPracticeAttempt a
        WHERE a.enrollment.student.id = :studentId
          AND a.practiceExam.id = :practiceExamId
          AND a.id <> :attemptId
          AND a.score IS NOT NULL
          AND a.startTime < :before
        ORDER BY a.startTime DESC, a.id DESC
    """)
    List<StudentPracticeAttempt> findPreviousScored(@Param("studentId") Integer studentId,
                                                    @Param("practiceExamId") Integer practiceExamId,
                                                    @Param("attemptId") Integer attemptId,
                                                    @Param("before") LocalDateTime before,
                                                    Pageable pageable);

    @Query("""
        SELECT a.language FROM StudentPracticeAttempt a
        WHERE a.enrollment.student.id = :studentId AND a.language IS NOT NULL
        ORDER BY a.startTime DESC
    """)
    List<String> findRecentLanguagesForStudent(@Param("studentId") Integer studentId, Pageable pageable);
}

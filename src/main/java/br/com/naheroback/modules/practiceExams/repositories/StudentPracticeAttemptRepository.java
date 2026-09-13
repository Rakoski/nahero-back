package br.com.naheroback.modules.practiceExams.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentPracticeAttemptRepository extends BaseRepository<StudentPracticeAttempt, Integer> {

    @Query("""
        SELECT a FROM StudentPracticeAttempt a
        LEFT JOIN FETCH a.practiceExam
        LEFT JOIN FETCH a.attemptStatus
        WHERE a.enrollment.student.id = :studentId
    """)
    List<StudentPracticeAttempt> findAllForStudentDashboard(@Param("studentId") Integer studentId);

    @Query("""
        SELECT a.language FROM StudentPracticeAttempt a
        WHERE a.enrollment.student.id = :studentId AND a.language IS NOT NULL
        ORDER BY a.startTime DESC
    """)
    List<String> findRecentLanguagesForStudent(@Param("studentId") Integer studentId, Pageable pageable);
}

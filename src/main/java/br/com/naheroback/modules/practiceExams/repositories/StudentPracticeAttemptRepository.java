package br.com.naheroback.modules.practiceExams.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
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
}

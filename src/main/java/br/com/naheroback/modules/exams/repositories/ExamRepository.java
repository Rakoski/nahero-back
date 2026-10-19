package br.com.naheroback.modules.exams.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.exams.entities.Exam;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends BaseRepository<Exam, Integer> {
    List<Exam> findAllByTeacherIdOrderByCreatedAtDesc(Integer teacherId);
}

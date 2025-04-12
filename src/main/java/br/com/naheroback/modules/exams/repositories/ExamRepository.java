package br.com.naheroback.modules.exams.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.exams.entities.Exam;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamRepository extends BaseRepository<Exam, Integer> {
}
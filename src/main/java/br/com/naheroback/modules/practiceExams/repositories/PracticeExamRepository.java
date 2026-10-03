package br.com.naheroback.modules.practiceExams.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;

import java.util.List;
import java.util.Optional;

public interface PracticeExamRepository extends BaseRepository<PracticeExam, Integer> {
    Optional<PracticeExam> findBySlug(String slug);
    List<PracticeExam> findAllByIdIn(List<Integer> ids);
    boolean existsBySlug(String slug);
}

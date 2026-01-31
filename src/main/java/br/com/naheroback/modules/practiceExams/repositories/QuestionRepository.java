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

    @Query("SELECT q.id FROM Question q WHERE q.practiceExam.id = :practiceExamId")
    List<Integer> findAllIdsByPracticeExamId(@Param("practiceExamId") Integer practiceExamId);
}

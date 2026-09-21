package br.com.naheroback.modules.practiceExams.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.practiceExams.entities.Alternative;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlternativeRepository extends BaseRepository<Alternative, Integer> {
    int findVersionByBaseAlternativeId(int baseAlternativeId);
    List<Alternative> findAllByQuestionId(int questionId);

    @Query("""
            SELECT a FROM Alternative a
            WHERE a.question.id IN :questionIds
              AND COALESCE(a.isActive, true) = true
            ORDER BY a.id ASC
            """)
    List<Alternative> findAllActiveByQuestionIdIn(@Param("questionIds") List<Integer> questionIds);
}

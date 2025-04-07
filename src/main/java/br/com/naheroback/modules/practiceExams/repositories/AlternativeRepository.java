package br.com.naheroback.modules.practiceExams.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.practiceExams.entities.Alternative;

import java.util.List;

public interface AlternativeRepository extends BaseRepository<Alternative, Integer> {
    int findVersionByBaseAlternativeId(int baseAlternativeId);
    List<Alternative> findAllByQuestionId(int questionId);
}

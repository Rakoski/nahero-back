package br.com.naheroback.modules.practiceExams.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.practiceExams.entities.Question;

import java.util.List;

public interface QuestionRepository extends BaseRepository<Question, Integer> {
    int findVersionByBaseQuestionId(int baseQuestionId);
    List<Question> findAllByPracticeExamId(int practiceExamId);
}

package br.com.naheroback.modules.questionLists.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.questionLists.entities.QuestionList;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(exported = false)
public interface QuestionListRepository extends BaseRepository<QuestionList, Integer> {
    Optional<QuestionList> findByIdAndStudentId(Integer id, Integer studentId);

    List<QuestionList> findAllByStudentIdOrderByCreatedAtDesc(Integer studentId);

    long countByStudentId(Integer studentId);
}

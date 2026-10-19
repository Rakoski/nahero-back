package br.com.naheroback.modules.questionLists.repositories;

import br.com.naheroback.modules.questionLists.entities.QuestionListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Collection;
import java.util.List;

@RepositoryRestResource(exported = false)
public interface QuestionListItemRepository extends JpaRepository<QuestionListItem, Integer> {
    List<QuestionListItem> findAllByQuestionListIdOrderByPositionAsc(Integer questionListId);

    List<QuestionListItem> findAllByQuestionListIdIn(Collection<Integer> questionListIds);

    boolean existsByQuestionListIdAndQuestionId(Integer questionListId, Integer questionId);

    @Modifying
    @Query("DELETE FROM QuestionListItem i WHERE i.questionListId = :questionListId AND i.questionId = :questionId")
    int deleteByQuestionListIdAndQuestionId(@Param("questionListId") Integer questionListId,
                                            @Param("questionId") Integer questionId);
}

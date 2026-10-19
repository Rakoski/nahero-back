package br.com.naheroback.modules.practiceExams.repositories;

import br.com.naheroback.modules.practiceExams.entities.QuestionStudyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface QuestionStudyAnswerRepository extends JpaRepository<QuestionStudyAnswer, Integer> {
}

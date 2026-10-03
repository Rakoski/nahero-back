package br.com.naheroback.modules.practiceExams.repositories;

import br.com.naheroback.modules.practiceExams.entities.AttemptFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(exported = false)
public interface AttemptFeedbackRepository extends JpaRepository<AttemptFeedback, Integer> {
    Optional<AttemptFeedback> findByAttemptId(Integer attemptId);

    boolean existsByAttemptId(Integer attemptId);
}

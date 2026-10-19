package br.com.naheroback.modules.emailOutbox.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.emailOutbox.entities.EmailOutbox;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.time.LocalDateTime;
import java.util.Optional;

@RepositoryRestResource(exported = false)
public interface EmailOutboxRepository extends BaseRepository<EmailOutbox, Integer> {

    @Query(value = """
        SELECT *
        FROM email_outbox
        WHERE status = 'PENDING'
          AND next_attempt_at <= :now
          AND deleted_at IS NULL
        ORDER BY next_attempt_at, id
        LIMIT 1
        FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    Optional<EmailOutbox> lockNextDue(@Param("now") LocalDateTime now);
}

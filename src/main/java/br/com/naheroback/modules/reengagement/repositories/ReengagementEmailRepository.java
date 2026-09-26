package br.com.naheroback.modules.reengagement.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.reengagement.entities.ReengagementEmail;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReengagementEmailRepository extends BaseRepository<ReengagementEmail, Integer> {

    @Query("""
        SELECT r.user.id AS userId,
               r.emailType AS emailType,
               r.status AS status,
               r.sentAt AS sentAt
        FROM ReengagementEmail r
        WHERE r.user.id IN :userIds
          AND r.sentAt > :sentAt
    """)
    List<ReengagementEmailHistory> findHistoryForUsers(
            @Param("userIds") Collection<Integer> userIds,
            @Param("sentAt") LocalDateTime sentAt
    );
}

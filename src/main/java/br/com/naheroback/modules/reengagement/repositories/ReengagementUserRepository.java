package br.com.naheroback.modules.reengagement.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.user.entities.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReengagementUserRepository extends BaseRepository<User, Integer> {

    @Query("""
        SELECT u.id AS userId,
               u.name AS name,
               u.email AS email,
               COALESCE(MAX(a.startTime), u.createdAt) AS lastActivityAt
        FROM User u
        LEFT JOIN Enrollment e ON e.student.id = u.id
        LEFT JOIN StudentPracticeAttempt a ON a.enrollment.id = e.id
        WHERE u.email IS NOT NULL
          AND u.emailConfirmedAt IS NOT NULL
          AND u.reengagementOptedOutAt IS NULL
          AND EXISTS (
              SELECT 1 FROM User roleOwner JOIN roleOwner.roles ownedRole
              WHERE roleOwner.id = u.id AND ownedRole.name = 'IS_STUDENT'
          )
          AND NOT EXISTS (
              SELECT 1 FROM ReengagementEmail r
              WHERE r.user.id = u.id AND r.sentAt > :cooldownStart
          )
        GROUP BY u.id, u.name, u.email, u.createdAt
        HAVING COALESCE(MAX(a.startTime), u.createdAt) > :campaignStart
           AND COALESCE(MAX(a.startTime), u.createdAt) <= :inactiveSince
        ORDER BY u.id
    """)
    List<ReengagementCandidate> findCampaignCandidates(
            @Param("campaignStart") LocalDateTime campaignStart,
            @Param("inactiveSince") LocalDateTime inactiveSince,
            @Param("cooldownStart") LocalDateTime cooldownStart,
            Pageable pageable
    );
}

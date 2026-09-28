package br.com.naheroback.modules.reengagement.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.reengagement.entities.enums.ReengagementEmailStatus;
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
              WHERE r.user.id = u.id AND r.sentAt > :cooldownStart AND r.status = :deliveredStatus
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
            @Param("deliveredStatus") ReengagementEmailStatus deliveredStatus,
            Pageable pageable
    );

    @Query(value = """
        WITH student AS (
            SELECT u.id AS id,
                   u.reengagement_opted_out_at IS NOT NULL AS opted_out,
                   (u.email IS NULL OR u.email_confirmed_at IS NULL) AS unreachable,
                   COALESCE(MAX(a.start_time), u.created_at) AS last_activity_at,
                   EXISTS (
                       SELECT 1 FROM reengagement_emails re
                       WHERE re.user_id = u.id
                         AND re.deleted_at IS NULL
                         AND re.status = 'SENT'
                         AND re.sent_at > :cooldownStart
                   ) AS in_cooldown
            FROM users u
            JOIN user_roles ur ON ur.user_id = u.id
            JOIN roles ro ON ro.id = ur.role_id AND ro.name = 'IS_STUDENT' AND ro.deleted_at IS NULL
            LEFT JOIN enrollments e ON e.student_id = u.id AND e.deleted_at IS NULL
            LEFT JOIN student_practice_attempts a ON a.enrollment_id = e.id AND a.deleted_at IS NULL
            WHERE u.deleted_at IS NULL
            GROUP BY u.id, u.reengagement_opted_out_at, u.email, u.email_confirmed_at, u.created_at
        ),
        reachable AS (
            SELECT * FROM student WHERE NOT opted_out AND NOT unreachable
        )
        SELECT json_build_object(
            'students', (SELECT count(*) FROM student),
            'optedOut', (SELECT count(*) FROM student WHERE opted_out),
            'unreachable', (SELECT count(*) FROM student WHERE NOT opted_out AND unreachable),
            'stillActive', (SELECT count(*) FROM reachable WHERE last_activity_at > :inactiveSince),
            'outsideCampaignWindow', (SELECT count(*) FROM reachable WHERE last_activity_at <= :campaignStart),
            'inCooldown', (SELECT count(*) FROM reachable
                           WHERE last_activity_at > :campaignStart
                             AND last_activity_at <= :inactiveSince
                             AND in_cooldown),
            'eligible', (SELECT count(*) FROM reachable
                         WHERE last_activity_at > :campaignStart
                           AND last_activity_at <= :inactiveSince
                           AND NOT in_cooldown)
        )::text
    """, nativeQuery = true)
    String findEligibilityFunnel(
            @Param("campaignStart") LocalDateTime campaignStart,
            @Param("inactiveSince") LocalDateTime inactiveSince,
            @Param("cooldownStart") LocalDateTime cooldownStart
    );
}

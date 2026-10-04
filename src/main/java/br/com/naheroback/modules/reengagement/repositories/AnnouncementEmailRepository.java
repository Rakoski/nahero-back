package br.com.naheroback.modules.reengagement.repositories;

import br.com.naheroback.modules.reengagement.entities.AnnouncementEmail;
import br.com.naheroback.modules.reengagement.entities.enums.ReengagementEmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(exported = false)
public interface AnnouncementEmailRepository extends JpaRepository<AnnouncementEmail, Integer> {

    @Query("""
        SELECT u.id FROM User u
        WHERE u.email IS NOT NULL
          AND u.emailConfirmedAt IS NOT NULL
          AND u.reengagementOptedOutAt IS NULL
          AND EXISTS (
              SELECT 1 FROM User roleOwner JOIN roleOwner.roles ownedRole
              WHERE roleOwner.id = u.id AND ownedRole.name = 'IS_STUDENT'
          )
          AND NOT EXISTS (
              SELECT 1 FROM AnnouncementEmail a
              WHERE a.user.id = u.id AND a.campaign = :campaign AND a.status = :deliveredStatus
          )
        ORDER BY u.id
    """)
    List<Integer> findRecipientIds(@Param("campaign") String campaign,
                                   @Param("deliveredStatus") ReengagementEmailStatus deliveredStatus);

    Optional<AnnouncementEmail> findByUserIdAndCampaign(Integer userId, String campaign);
}

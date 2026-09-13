package br.com.naheroback.modules.reengagement.entities;

import br.com.naheroback.common.entities.BaseEntity;
import br.com.naheroback.modules.reengagement.entities.enums.ReengagementEmailType;
import br.com.naheroback.modules.user.entities.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reengagement_emails", indexes = {
    @Index(name = "idx_reengagement_emails_user", columnList = "user_id"),
    @Index(name = "idx_reengagement_emails_sent_at", columnList = "sent_at")
})
@SQLDelete(sql = "UPDATE reengagement_emails SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class ReengagementEmail extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_type", nullable = false, length = 64)
    private ReengagementEmailType emailType;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "campaign_started_at", nullable = false)
    private LocalDateTime campaignStartedAt;
}

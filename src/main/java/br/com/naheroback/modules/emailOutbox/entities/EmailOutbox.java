package br.com.naheroback.modules.emailOutbox.entities;

import br.com.naheroback.common.entities.BaseEntity;
import br.com.naheroback.modules.emailOutbox.entities.enums.EmailOutboxStatus;
import br.com.naheroback.modules.emailOutbox.entities.enums.EmailOutboxType;
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
@Table(name = "email_outbox", indexes = {
    @Index(name = "idx_email_outbox_user", columnList = "user_id")
})
@SQLDelete(sql = "UPDATE email_outbox SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class EmailOutbox extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_type", nullable = false, length = 64)
    private EmailOutboxType emailType;

    @Column(name = "locale", nullable = false, length = 35)
    private String locale;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private EmailOutboxStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}

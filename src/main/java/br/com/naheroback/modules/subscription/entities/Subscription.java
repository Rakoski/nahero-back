package br.com.naheroback.modules.subscription.entities;

import br.com.naheroback.common.entities.BaseEntity;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.providers.payment.PaymentProviderName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "subscriptions",
    indexes = {
        @Index(name = "idx_subscriptions_user_id", columnList = "user_id"),
        @Index(name = "idx_subscriptions_current_period_end", columnList = "current_period_end")
    },
    uniqueConstraints = @UniqueConstraint(
        name = "uq_subscriptions_provider_external",
        columnNames = {"provider", "external_subscription_id"}
    )
)
public class Subscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private PaymentProviderName provider;

    @Column(name = "external_subscription_id", nullable = false)
    private String externalSubscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SubscriptionStatus status;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd;
}

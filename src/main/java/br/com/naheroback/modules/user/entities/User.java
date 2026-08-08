package br.com.naheroback.modules.user.entities;

import br.com.naheroback.common.entities.BaseEntity;
import br.com.naheroback.providers.payment.PaymentProviderName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_cpf", columnList = "cpf"),
    @Index(name = "idx_users_deleted_at", columnList = "deleted_at"),
    @Index(name = "idx_users_email_confirmed_at", columnList = "email_confirmed_at"),
    @Index(name = "idx_users_external_customer_id", columnList = "external_customer_id"),
    @Index(name = "idx_users_payment_provider", columnList = "payment_provider"),
    @Index(name = "idx_users_address_id", columnList = "address_id")
})
@SQLDelete(sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class User extends BaseEntity {

    private static final int MAXIMUM_BIO_VALUE = 500;

    @Column
    private String name;

    @Column(unique = true)
    private String email;

    @Column(unique = true, length = 20)
    private String cpf;

    @Column(length = MAXIMUM_BIO_VALUE)
    private String bio;

    private String password;
    private String phone;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "email_confirmed_at")
    private LocalDateTime emailConfirmedAt;

    @Column(name = "free_tries_left", nullable = false)
    private Integer freeTriesLeft = 1;

    @Column(name = "external_customer_id")
    private String externalCustomerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_provider", length = 32)
    private PaymentProviderName paymentProvider;

    @Column(name = "forgot_password_token")
    private String forgotPasswordToken;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "address_id")
    private Address address;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }
}

package br.com.naheroback.modules.user.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.providers.payment.PaymentProviderName;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends BaseRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    Optional<User> findByCpf(String cpf);
    Optional<User> findByForgotPasswordToken(String forgotPasswordToken);
    Optional<User> findByPaymentProviderAndExternalCustomerId(PaymentProviderName provider, String externalCustomerId);

    @Modifying
    @Query("UPDATE User u SET u.freeTriesLeft = u.freeTriesLeft - 1 WHERE u.id = :id AND u.freeTriesLeft > 0")
    int decrementFreeTriesIfAvailable(@Param("id") Integer id);
}

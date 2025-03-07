package br.com.naheroback.modules.user.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.user.entities.User;

import java.util.Optional;

public interface UserRepository extends BaseRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    Optional<User> findByCpf(String cpf);
    Optional<User> findByPassportNumber(String passportNumber);
}

package br.com.naheroback.modules.user.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.user.entities.Role;

import java.util.Optional;

public interface RoleRepository extends BaseRepository<Role, Integer> {
    Optional<Role> findByName(String name);
}

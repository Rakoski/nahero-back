package br.com.naheroback.modules.user.useCases.role.link;

import br.com.naheroback.common.exceptions.custom.DuplicateException;
import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.user.entities.Role;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.RoleRepository;
import br.com.naheroback.modules.user.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LinkUserRoleUseCase {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional
    @Secured("IS_ADMIN")
    public void execute(LinkUserRoleRequest request) {
        User user = userRepository.findById(request.userId())
            .orElseThrow(() -> NotFoundException.with(User.class, "id", request.userId()));

        Set<Integer> existingRoleIds = user.getRoles().stream()
            .map(Role::getId)
            .collect(Collectors.toSet());

        request.roleIds().forEach(roleId -> {
            if (existingRoleIds.contains(roleId)) {
                throw DuplicateException.with(
                        User.class, "userId and roleId",
                        request.userId() + " and " + roleId
                );
            }
        });

        request.roleIds().stream()
                .map(roleId -> roleRepository.findById(roleId)
                        .orElseThrow(() -> NotFoundException.with(Role.class, "id", roleId)))

                .forEach(role -> user.getRoles().add(role));

        userRepository.save(user);
    }

}

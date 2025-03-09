package br.com.naheroback.modules.user.useCases.permission.link;

import br.com.naheroback.common.exceptions.custom.DuplicateException;
import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.user.entities.*;
import br.com.naheroback.modules.user.repositories.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LinkRolePermissionUseCase {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional
    @Secured("IS_ADMIN")
    public void execute(LinkRolePermissionRequest request) {
        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> NotFoundException.with(Role.class, "id", request.roleId()));

        Set<Integer> existingPermissionIds = role.getPermissions().stream()
                .map(Permission::getId)
                .collect(Collectors.toSet());

        request.permissionIds().forEach(permissionId -> {
            if (existingPermissionIds.contains(permissionId)) {
                throw DuplicateException.with(
                        Role.class, "roleId and permissionId",
                        request.roleId() + " and " + permissionId
                );
            }
        });

        request.permissionIds().stream()
                .map(permissionId -> permissionRepository.findById(permissionId)
                        .orElseThrow(() -> NotFoundException.with(Permission.class, "id", permissionId)))

                .forEach(permission -> role.getPermissions().add(permission));

        roleRepository.save(role);
    }

}

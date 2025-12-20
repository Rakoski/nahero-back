package br.com.naheroback.modules.user.useCases.permission.link;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record LinkRolePermissionRequest(
    @NotNull(message = "{permission.roleid.required}") @Positive Integer roleId,
    @Valid @NotEmpty(message = "{permission.permissionids.required}") List<Integer> permissionIds
) {}

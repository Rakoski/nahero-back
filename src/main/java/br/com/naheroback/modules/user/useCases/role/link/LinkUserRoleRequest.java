package br.com.naheroback.modules.user.useCases.role.link;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record LinkUserRoleRequest(
    @NotNull(message = "{role.userid.required}") @Positive Integer userId,
    @Valid @NotEmpty(message = "{role.roleids.required}") List<Integer> roleIds
) {}

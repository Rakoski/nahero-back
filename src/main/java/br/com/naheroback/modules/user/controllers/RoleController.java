package br.com.naheroback.modules.user.controllers;

import br.com.naheroback.modules.user.useCases.role.link.LinkUserRoleRequest;
import br.com.naheroback.modules.user.useCases.role.link.LinkUserRoleUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/roles")
public class RoleController {
    private final LinkUserRoleUseCase linkUserRoleUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void linkRoles(@Valid @RequestBody LinkUserRoleRequest request) {
        linkUserRoleUseCase.execute(request);
    }
}

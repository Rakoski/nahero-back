package br.com.naheroback.modules.user.controllers;

import br.com.naheroback.modules.user.useCases.permission.link.LinkRolePermissionRequest;
import br.com.naheroback.modules.user.useCases.permission.link.LinkRolePermissionUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/permissions")
public class PermissionController {
    private final LinkRolePermissionUseCase linkRolePermissionUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void linkRolePermissions(@Valid @RequestBody LinkRolePermissionRequest request) {
        linkRolePermissionUseCase.execute(request);
    }
}

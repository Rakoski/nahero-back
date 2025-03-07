package br.com.naheroback.modules.user.controllers;

import br.com.naheroback.modules.user.useCases.user.create.CreateUserRequest;
import br.com.naheroback.modules.user.useCases.user.create.CreateUserResponse;
import br.com.naheroback.modules.user.useCases.user.create.CreateUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final CreateUserUseCase createUserUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateUserResponse createUser(@RequestBody CreateUserRequest request) {
        return this.createUserUseCase.execute(request);
    }

}

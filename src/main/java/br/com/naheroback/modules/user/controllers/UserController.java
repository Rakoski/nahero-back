package br.com.naheroback.modules.user.controllers;

import br.com.naheroback.modules.user.useCases.user.create.CreateUserRequest;
import br.com.naheroback.modules.user.useCases.user.create.CreateUserResponse;
import br.com.naheroback.modules.user.useCases.user.create.CreateUserUseCase;
import br.com.naheroback.modules.user.useCases.user.getById.GetUserByIdResponse;
import br.com.naheroback.modules.user.useCases.user.getById.GetUserByIdUseCase;
import br.com.naheroback.modules.user.useCases.user.resendEmailVerification.ResendEmailVerificationRequest;
import br.com.naheroback.modules.user.useCases.user.resendEmailVerification.ResendEmailVerificationUseCase;
import br.com.naheroback.modules.user.useCases.user.verifyEmail.VerifyEmailRequest;
import br.com.naheroback.modules.user.useCases.user.verifyEmail.VerifyEmailUseCase;
import br.com.naheroback.common.ratelimit.RateLimitProperties;
import br.com.naheroback.common.ratelimit.RateLimitService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final CreateUserUseCase createUserUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final ResendEmailVerificationUseCase resendEmailVerificationUseCase;
    private final RateLimitService rateLimitService;
    private final RateLimitProperties rateLimitProperties;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateUserResponse createUser(@RequestBody CreateUserRequest request) {
        return this.createUserUseCase.execute(request);
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        this.verifyEmailUseCase.execute(request);
    }

    @PostMapping("/verify-email/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendEmailVerification(@Valid @RequestBody ResendEmailVerificationRequest request) {
        this.rateLimitService.consume(rateLimitProperties.recipientPolicy(), request.email());

        this.resendEmailVerificationUseCase.execute(request);
    }

    @GetMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    public GetUserByIdResponse getById(@PathVariable @Positive Integer id) {
        return this.getUserByIdUseCase.execute(id);
    }
}

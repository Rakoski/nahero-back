package br.com.naheroback.modules.enrollment.controllers;

import br.com.naheroback.modules.enrollment.useCases.enrollment.create.CreateEnrollmentRequest;
import br.com.naheroback.modules.enrollment.useCases.enrollment.create.CreateEnrollmentUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/enrollments")
public class EnrollmentController {
    private final CreateEnrollmentUseCase createEnrollmentUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody CreateEnrollmentRequest request) {
        createEnrollmentUseCase.execute(request);
    }
}

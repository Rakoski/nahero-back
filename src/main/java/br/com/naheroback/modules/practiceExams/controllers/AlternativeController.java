package br.com.naheroback.modules.practiceExams.controllers;

import br.com.naheroback.modules.practiceExams.useCases.alternative.create.CreateAlternativeRequest;
import br.com.naheroback.modules.practiceExams.useCases.alternative.create.CreateAlternativeUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/alternatives")
public class AlternativeController {
    private final CreateAlternativeUseCase createAlternativeUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Integer create(@Valid @RequestBody CreateAlternativeRequest request) {
        return createAlternativeUseCase.execute(request);
    }
}

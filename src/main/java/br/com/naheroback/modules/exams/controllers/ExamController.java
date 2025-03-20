package br.com.naheroback.modules.exams.controllers;

import br.com.naheroback.modules.exams.useCases.create.CreateExamRequest;
import br.com.naheroback.modules.exams.useCases.create.CreateExamResponse;
import br.com.naheroback.modules.exams.useCases.create.CreateExamUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {
    
    private final CreateExamUseCase createExamUseCase;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateExamResponse create(@RequestBody @Valid CreateExamRequest request) {
        return createExamUseCase.execute(request);
    }
}
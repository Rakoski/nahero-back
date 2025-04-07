package br.com.naheroback.modules.practiceExams.controllers;

import br.com.naheroback.modules.practiceExams.useCases.alternative.create.CreateAlternativeRequest;
import br.com.naheroback.modules.practiceExams.useCases.alternative.create.CreateAlternativeUseCase;
import br.com.naheroback.modules.practiceExams.useCases.alternative.listByQuestion.ListAlternativeByQuestionResponse;
import br.com.naheroback.modules.practiceExams.useCases.alternative.listByQuestion.ListAlternativeByQuestionUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/alternatives")
public class AlternativeController {
    private final CreateAlternativeUseCase createAlternativeUseCase;
    private final ListAlternativeByQuestionUseCase listAlternativeByQuestionUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Integer create(@Valid @RequestBody CreateAlternativeRequest request) {
        return createAlternativeUseCase.execute(request);
    }

    @GetMapping("/{questionId}")
    @ResponseStatus(HttpStatus.OK)
    public List<ListAlternativeByQuestionResponse> listByQuestion(@PathVariable @Positive Integer questionId) {
        return listAlternativeByQuestionUseCase.execute(questionId);
    }
}

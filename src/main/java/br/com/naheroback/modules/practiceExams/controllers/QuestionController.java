package br.com.naheroback.modules.practiceExams.controllers;

import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.useCases.question.create.CreateQuestionRequest;
import br.com.naheroback.modules.practiceExams.useCases.question.create.CreateQuestionUseCase;
import br.com.naheroback.modules.practiceExams.useCases.question.listStudent.ListQuestionsByStudentResponse;
import br.com.naheroback.modules.practiceExams.useCases.question.listStudent.ListQuestionsByStudentUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/questions")
public class QuestionController {
    private final CreateQuestionUseCase createQuestionUseCase;
    private final ListQuestionsByStudentUseCase listQuestionsByStudentUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody CreateQuestionRequest request) {
        createQuestionUseCase.execute(request);
    }

    @GetMapping("/list-student/{practiceExamId}")
    @ResponseStatus(HttpStatus.OK)
    public List<ListQuestionsByStudentResponse> listStudent(@PathVariable @Positive Integer practiceExamId) {
        return listQuestionsByStudentUseCase.execute(practiceExamId);
    }

}

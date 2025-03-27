package br.com.naheroback.modules.practiceExams.controllers;

import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.useCases.practiceExams.create.CreatePracticeExamRequest;
import br.com.naheroback.modules.practiceExams.useCases.practiceExams.create.CreatePracticeExamUseCase;
import br.com.naheroback.modules.practiceExams.useCases.practiceExams.list.ListPracticeExamsResponse;
import br.com.naheroback.modules.practiceExams.useCases.practiceExams.list.ListPracticeExamsUseCase;
import com.querydsl.core.types.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/practice-exams")
public class PracticeExamController {
    private final CreatePracticeExamUseCase createPracticeExamUseCase;
    private final ListPracticeExamsUseCase listPracticeExamsUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody CreatePracticeExamRequest request) {
        createPracticeExamUseCase.execute(request);
    }

    @GetMapping("/list")
    @ResponseStatus(HttpStatus.OK)
    public Page<ListPracticeExamsResponse> list(
            @QuerydslPredicate(root = PracticeExam.class) Predicate predicate,
            Pageable pagination
    ) {
        return listPracticeExamsUseCase.execute(predicate, pagination);
    }
}

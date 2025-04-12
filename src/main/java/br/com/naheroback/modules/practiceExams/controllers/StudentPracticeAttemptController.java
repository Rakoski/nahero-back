package br.com.naheroback.modules.practiceExams.controllers;

import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.create.CreateStudentPracticeAttemptRequest;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.create.CreateStudentPracticeAttemptUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.finish.FinishStudentPracticeAttemptRequest;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.finish.FinishStudentPracticeAttemptUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/student-practice-attempts")
public class StudentPracticeAttemptController {
    private final CreateStudentPracticeAttemptUseCase createStudentPracticeAttemptUseCase;
    private final FinishStudentPracticeAttemptUseCase finishStudentPracticeAttemptUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Integer create(@Valid @RequestBody CreateStudentPracticeAttemptRequest request) {
        return createStudentPracticeAttemptUseCase.execute(request);
    }

    @PutMapping("/finish")
    @ResponseStatus(HttpStatus.OK)
    public Boolean finish(@Valid @RequestBody FinishStudentPracticeAttemptRequest request) {
        return finishStudentPracticeAttemptUseCase.execute(request);
    }
}

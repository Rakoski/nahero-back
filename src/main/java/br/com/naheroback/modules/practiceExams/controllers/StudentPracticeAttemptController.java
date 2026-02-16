package br.com.naheroback.modules.practiceExams.controllers;

import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.create.CreateStudentPracticeAttemptRequest;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.create.CreateStudentPracticeAttemptUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.finish.FinishStudentPracticeAttemptRequest;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.finish.FinishStudentPracticeAttemptUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getResult.GetResultResponse;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getResult.GetResultUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/student-practice-attempts")
public class StudentPracticeAttemptController {
    private final CreateStudentPracticeAttemptUseCase createStudentPracticeAttemptUseCase;
    private final FinishStudentPracticeAttemptUseCase finishStudentPracticeAttemptUseCase;
    private final GetResultUseCase getResultUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Integer create(@Valid @RequestBody CreateStudentPracticeAttemptRequest request) {
        return createStudentPracticeAttemptUseCase.execute(request);
    }

    @PutMapping("/finish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void finish(@Valid @RequestBody FinishStudentPracticeAttemptRequest request) {
        finishStudentPracticeAttemptUseCase.execute(request);
    }

    @GetMapping("/{attemptId}/result")
    public ResponseEntity<GetResultResponse> getResult(@PathVariable Integer attemptId) {
        GetResultResponse result = getResultUseCase.execute(attemptId);
        return ResponseEntity.ok(result);
    }
}

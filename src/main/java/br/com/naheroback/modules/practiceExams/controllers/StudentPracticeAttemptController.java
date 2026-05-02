package br.com.naheroback.modules.practiceExams.controllers;

import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.create.CreateStudentPracticeAttemptRequest;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.create.CreateStudentPracticeAttemptUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.finish.FinishStudentPracticeAttemptRequest;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.finish.FinishStudentPracticeAttemptUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getHistory.GetHistoryFilterDTO;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getHistory.GetHistoryResponse;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getHistory.GetHistoryUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getResult.GetResultResponse;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getResult.GetResultUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getDashboardSummary.GetDashboardSummaryResponse;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getDashboardSummary.GetDashboardSummaryUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/student-practice-attempts")
public class StudentPracticeAttemptController {
    private final CreateStudentPracticeAttemptUseCase createStudentPracticeAttemptUseCase;
    private final FinishStudentPracticeAttemptUseCase finishStudentPracticeAttemptUseCase;
    private final GetResultUseCase getResultUseCase;
    private final GetHistoryUseCase getHistoryUseCase;
    private final GetDashboardSummaryUseCase getDashboardSummaryUseCase;

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

    @GetMapping("/history")
    public Page<GetHistoryResponse> getHistory(
            @RequestParam(required = false) Integer practiceExamId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) Integer score,
            Pageable pageable) {
        GetHistoryFilterDTO filter = new GetHistoryFilterDTO(practiceExamId,
                startDate != null ? startDate.atStartOfDay() : null,
                endDate != null ? endDate.atTime(23, 59, 59) : null,
                score);
        return getHistoryUseCase.execute(filter, pageable);
    }

    @GetMapping("/dashboard-summary")
    public GetDashboardSummaryResponse getDashboardSummary() {
        return getDashboardSummaryUseCase.execute();
    }
}

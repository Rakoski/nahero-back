package br.com.naheroback.modules.practiceExams.controllers;

import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.abandon.AbandonStudentPracticeAttemptUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.create.CreateStudentPracticeAttemptRequest;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.create.CreateStudentPracticeAttemptUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.finish.FinishStudentPracticeAttemptRequest;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.finish.FinishStudentPracticeAttemptUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getHistory.GetHistoryFilterDTO;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getHistory.GetHistoryResponse;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getHistory.GetHistoryUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getInProgress.GetInProgressAttemptUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getState.GetAttemptStateResponse;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getState.GetAttemptStateUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.saveProgress.SaveAttemptProgressRequest;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.saveProgress.SaveAttemptProgressUseCase;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.timeout.TimeOutStudentPracticeAttemptRequest;
import br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.timeout.TimeOutStudentPracticeAttemptUseCase;
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
import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping("/student-practice-attempts")
public class StudentPracticeAttemptController {
    private final CreateStudentPracticeAttemptUseCase createStudentPracticeAttemptUseCase;
    private final FinishStudentPracticeAttemptUseCase finishStudentPracticeAttemptUseCase;
    private final AbandonStudentPracticeAttemptUseCase abandonStudentPracticeAttemptUseCase;
    private final TimeOutStudentPracticeAttemptUseCase timeOutStudentPracticeAttemptUseCase;
    private final GetResultUseCase getResultUseCase;
    private final GetHistoryUseCase getHistoryUseCase;
    private final GetDashboardSummaryUseCase getDashboardSummaryUseCase;
    private final GetAttemptStateUseCase getAttemptStateUseCase;
    private final GetInProgressAttemptUseCase getInProgressAttemptUseCase;
    private final SaveAttemptProgressUseCase saveAttemptProgressUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Integer create(@Valid @RequestBody CreateStudentPracticeAttemptRequest request, Locale locale) {
        return createStudentPracticeAttemptUseCase.execute(request, locale);
    }

    @PutMapping("/finish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void finish(@Valid @RequestBody FinishStudentPracticeAttemptRequest request) {
        finishStudentPracticeAttemptUseCase.execute(request);
    }

    @PutMapping("/{attemptId}/abandon")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void abandon(@PathVariable Integer attemptId) {
        abandonStudentPracticeAttemptUseCase.execute(attemptId);
    }

    @PutMapping("/{attemptId}/timeout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void timeout(@PathVariable Integer attemptId,
                        @Valid @RequestBody TimeOutStudentPracticeAttemptRequest request) {
        timeOutStudentPracticeAttemptUseCase.execute(attemptId, request);
    }

    @PutMapping("/{attemptId}/progress")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveProgress(@PathVariable Integer attemptId,
                             @Valid @RequestBody SaveAttemptProgressRequest request) {
        saveAttemptProgressUseCase.execute(attemptId, request);
    }

    @GetMapping("/in-progress")
    public ResponseEntity<GetAttemptStateResponse> getInProgress() {
        return getInProgressAttemptUseCase.execute()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{attemptId}/state")
    public GetAttemptStateResponse getState(@PathVariable Integer attemptId) {
        return getAttemptStateUseCase.execute(attemptId);
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

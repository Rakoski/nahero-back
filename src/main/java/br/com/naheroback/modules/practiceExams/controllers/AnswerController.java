package br.com.naheroback.modules.practiceExams.controllers;

import br.com.naheroback.modules.practiceExams.useCases.answer.listAnswered.AnswerFilterDTO;
import br.com.naheroback.modules.practiceExams.useCases.answer.listAnswered.ListAnsweredAnswersResponse;
import br.com.naheroback.modules.practiceExams.useCases.answer.listAnswered.ListAnsweredAnswersUseCase;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/answers")
public class AnswerController {
    private final ListAnsweredAnswersUseCase listAnsweredAnswersUseCase;

    @GetMapping("/attempt/{studentPracticeAttemptId}")
    public Page<ListAnsweredAnswersResponse> listByAttempt(
            @Positive @PathVariable Integer studentPracticeAttemptId,
            @RequestParam(required = false) Boolean isCorrect,
            @RequestParam(required = false) String questionContent,
            Pageable pageable) {
        AnswerFilterDTO filter = new AnswerFilterDTO(isCorrect, questionContent);
        return listAnsweredAnswersUseCase.execute(studentPracticeAttemptId, filter, pageable);
    }
}

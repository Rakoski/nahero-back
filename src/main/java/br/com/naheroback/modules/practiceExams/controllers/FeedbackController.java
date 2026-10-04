package br.com.naheroback.modules.practiceExams.controllers;

import br.com.naheroback.modules.practiceExams.useCases.feedback.answerPracticeQuestion.AnswerPracticeQuestionRequest;
import br.com.naheroback.modules.practiceExams.useCases.feedback.answerPracticeQuestion.AnswerPracticeQuestionResponse;
import br.com.naheroback.modules.practiceExams.useCases.feedback.answerPracticeQuestion.AnswerPracticeQuestionUseCase;
import br.com.naheroback.modules.practiceExams.useCases.feedback.getFeedbackPage.GetFeedbackPageResponse;
import br.com.naheroback.modules.practiceExams.useCases.feedback.getFeedbackPage.GetFeedbackPageUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/feedback")
public class FeedbackController {
    private final GetFeedbackPageUseCase getFeedbackPageUseCase;
    private final AnswerPracticeQuestionUseCase answerPracticeQuestionUseCase;

    @GetMapping
    public GetFeedbackPageResponse getPage(@RequestParam(required = false) String practiceExamSlug) {
        return getFeedbackPageUseCase.execute(practiceExamSlug);
    }

    @PostMapping("/practice-questions/{questionId}/answer")
    public AnswerPracticeQuestionResponse answer(@PathVariable Integer questionId,
                                                 @Valid @RequestBody AnswerPracticeQuestionRequest request) {
        return answerPracticeQuestionUseCase.execute(questionId, request);
    }
}

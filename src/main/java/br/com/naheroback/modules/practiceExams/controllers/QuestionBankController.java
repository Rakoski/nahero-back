package br.com.naheroback.modules.practiceExams.controllers;

import br.com.naheroback.modules.practiceExams.useCases.questionBank.answer.AnswerQuestionBankQuestionRequest;
import br.com.naheroback.modules.practiceExams.useCases.questionBank.answer.AnswerQuestionBankQuestionResponse;
import br.com.naheroback.modules.practiceExams.useCases.questionBank.answer.AnswerQuestionBankQuestionUseCase;
import br.com.naheroback.modules.practiceExams.useCases.questionBank.get.GetQuestionBankQuestionResponse;
import br.com.naheroback.modules.practiceExams.useCases.questionBank.get.GetQuestionBankQuestionUseCase;
import br.com.naheroback.modules.practiceExams.useCases.questionBank.getFilters.GetQuestionBankFiltersResponse;
import br.com.naheroback.modules.practiceExams.useCases.questionBank.getFilters.GetQuestionBankFiltersUseCase;
import br.com.naheroback.modules.practiceExams.useCases.questionBank.list.ListQuestionBankRequest;
import br.com.naheroback.modules.practiceExams.useCases.questionBank.list.ListQuestionBankResponse;
import br.com.naheroback.modules.practiceExams.useCases.questionBank.list.ListQuestionBankUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping("/question-bank")
public class QuestionBankController {
    public static final String CAPTCHA_HEADER = "X-Captcha-Token";

    private final GetQuestionBankFiltersUseCase getQuestionBankFiltersUseCase;
    private final ListQuestionBankUseCase listQuestionBankUseCase;
    private final GetQuestionBankQuestionUseCase getQuestionBankQuestionUseCase;
    private final AnswerQuestionBankQuestionUseCase answerQuestionBankQuestionUseCase;

    @GetMapping("/filters")
    public List<GetQuestionBankFiltersResponse> filters() {
        return getQuestionBankFiltersUseCase.execute();
    }

    @GetMapping
    public ListQuestionBankResponse list(@Valid ListQuestionBankRequest request,
                                         Locale locale,
                                         @RequestHeader(value = CAPTCHA_HEADER, required = false) String captchaToken) {
        return listQuestionBankUseCase.execute(request, locale, captchaToken);
    }

    @GetMapping("/questions/{questionId}")
    public GetQuestionBankQuestionResponse get(@Positive @PathVariable Integer questionId,
                                               @RequestHeader(value = CAPTCHA_HEADER, required = false) String captchaToken) {
        return getQuestionBankQuestionUseCase.execute(questionId, captchaToken);
    }

    @PostMapping("/questions/{questionId}/answer")
    public AnswerQuestionBankQuestionResponse answer(@Positive @PathVariable Integer questionId,
                                                     @Valid @RequestBody AnswerQuestionBankQuestionRequest request,
                                                     @RequestHeader(value = CAPTCHA_HEADER, required = false) String captchaToken) {
        return answerQuestionBankQuestionUseCase.execute(questionId, request, captchaToken);
    }
}

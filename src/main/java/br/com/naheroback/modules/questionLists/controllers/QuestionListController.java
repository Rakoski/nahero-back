package br.com.naheroback.modules.questionLists.controllers;

import br.com.naheroback.modules.practiceExams.controllers.QuestionBankController;
import br.com.naheroback.modules.questionLists.useCases.addQuestions.AddQuestionListQuestionsRequest;
import br.com.naheroback.modules.questionLists.useCases.addQuestions.AddQuestionListQuestionsResponse;
import br.com.naheroback.modules.questionLists.useCases.addQuestions.AddQuestionListQuestionsUseCase;
import br.com.naheroback.modules.questionLists.useCases.answer.AnswerQuestionListQuestionRequest;
import br.com.naheroback.modules.questionLists.useCases.answer.AnswerQuestionListQuestionResponse;
import br.com.naheroback.modules.questionLists.useCases.answer.AnswerQuestionListQuestionUseCase;
import br.com.naheroback.modules.questionLists.useCases.create.CreateQuestionListRequest;
import br.com.naheroback.modules.questionLists.useCases.create.CreateQuestionListResponse;
import br.com.naheroback.modules.questionLists.useCases.create.CreateQuestionListUseCase;
import br.com.naheroback.modules.questionLists.useCases.delete.DeleteQuestionListUseCase;
import br.com.naheroback.modules.questionLists.useCases.get.GetQuestionListResponse;
import br.com.naheroback.modules.questionLists.useCases.get.GetQuestionListUseCase;
import br.com.naheroback.modules.questionLists.useCases.listMine.ListMyQuestionListsResponse;
import br.com.naheroback.modules.questionLists.useCases.listMine.ListMyQuestionListsUseCase;
import br.com.naheroback.modules.questionLists.useCases.removeQuestion.RemoveQuestionListQuestionUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping("/question-lists")
public class QuestionListController {
    private final CreateQuestionListUseCase createQuestionListUseCase;
    private final ListMyQuestionListsUseCase listMyQuestionListsUseCase;
    private final GetQuestionListUseCase getQuestionListUseCase;
    private final DeleteQuestionListUseCase deleteQuestionListUseCase;
    private final AddQuestionListQuestionsUseCase addQuestionListQuestionsUseCase;
    private final RemoveQuestionListQuestionUseCase removeQuestionListQuestionUseCase;
    private final AnswerQuestionListQuestionUseCase answerQuestionListQuestionUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateQuestionListResponse create(@Valid @RequestBody CreateQuestionListRequest request,
                                             Locale locale,
                                             @RequestHeader(value = QuestionBankController.CAPTCHA_HEADER, required = false) String captchaToken) {
        return createQuestionListUseCase.execute(request, locale, captchaToken);
    }

    @GetMapping
    public List<ListMyQuestionListsResponse> listMine() {
        return listMyQuestionListsUseCase.execute();
    }

    @GetMapping("/{questionListId}")
    public GetQuestionListResponse get(@Positive @PathVariable Integer questionListId,
                                       @PositiveOrZero @RequestParam(required = false) Integer page,
                                       @Positive @RequestParam(required = false) Integer size,
                                       @RequestHeader(value = QuestionBankController.CAPTCHA_HEADER, required = false) String captchaToken) {
        return getQuestionListUseCase.execute(questionListId, page, size, captchaToken);
    }

    @DeleteMapping("/{questionListId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Positive @PathVariable Integer questionListId) {
        deleteQuestionListUseCase.execute(questionListId);
    }

    @PostMapping("/{questionListId}/questions")
    public AddQuestionListQuestionsResponse addQuestions(@Positive @PathVariable Integer questionListId,
                                                         @Valid @RequestBody AddQuestionListQuestionsRequest request,
                                                         Locale locale,
                                                         @RequestHeader(value = QuestionBankController.CAPTCHA_HEADER, required = false) String captchaToken) {
        return addQuestionListQuestionsUseCase.execute(questionListId, request, locale, captchaToken);
    }

    @DeleteMapping("/{questionListId}/questions/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeQuestion(@Positive @PathVariable Integer questionListId,
                               @Positive @PathVariable Integer questionId) {
        removeQuestionListQuestionUseCase.execute(questionListId, questionId);
    }

    @PostMapping("/{questionListId}/questions/{questionId}/answer")
    public AnswerQuestionListQuestionResponse answer(@Positive @PathVariable Integer questionListId,
                                                     @Positive @PathVariable Integer questionId,
                                                     @Valid @RequestBody AnswerQuestionListQuestionRequest request,
                                                     @RequestHeader(value = QuestionBankController.CAPTCHA_HEADER, required = false) String captchaToken) {
        return answerQuestionListQuestionUseCase.execute(questionListId, questionId, request, captchaToken);
    }
}

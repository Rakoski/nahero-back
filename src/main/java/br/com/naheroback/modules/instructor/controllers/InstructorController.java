package br.com.naheroback.modules.instructor.controllers;

import br.com.naheroback.modules.exams.useCases.create.CreateExamRequest;
import br.com.naheroback.modules.exams.useCases.create.CreateExamResponse;
import br.com.naheroback.modules.exams.useCases.create.CreateExamUseCase;
import br.com.naheroback.modules.instructor.useCases.addDomains.AddPracticeExamDomainsRequest;
import br.com.naheroback.modules.instructor.useCases.addDomains.AddPracticeExamDomainsUseCase;
import br.com.naheroback.modules.instructor.useCases.createPracticeExam.CreateInstructorPracticeExamRequest;
import br.com.naheroback.modules.instructor.useCases.createPracticeExam.CreateInstructorPracticeExamResponse;
import br.com.naheroback.modules.instructor.useCases.createPracticeExam.CreateInstructorPracticeExamUseCase;
import br.com.naheroback.modules.instructor.useCases.createQuestions.CreateInstructorQuestionsRequest;
import br.com.naheroback.modules.instructor.useCases.createQuestions.CreateInstructorQuestionsResponse;
import br.com.naheroback.modules.instructor.useCases.createQuestions.CreateInstructorQuestionsUseCase;
import br.com.naheroback.modules.instructor.useCases.deleteQuestion.DeleteInstructorQuestionUseCase;
import br.com.naheroback.modules.instructor.useCases.getPracticeExam.GetInstructorPracticeExamResponse;
import br.com.naheroback.modules.instructor.useCases.getPracticeExam.GetInstructorPracticeExamUseCase;
import br.com.naheroback.modules.instructor.useCases.listExams.ListInstructorExamsResponse;
import br.com.naheroback.modules.instructor.useCases.listExams.ListInstructorExamsUseCase;
import br.com.naheroback.modules.instructor.useCases.listPracticeExams.ListInstructorPracticeExamsResponse;
import br.com.naheroback.modules.instructor.useCases.listPracticeExams.ListInstructorPracticeExamsUseCase;
import br.com.naheroback.modules.instructor.useCases.listQuestions.ListInstructorQuestionsResponse;
import br.com.naheroback.modules.instructor.useCases.listQuestions.ListInstructorQuestionsUseCase;
import br.com.naheroback.modules.instructor.useCases.removeDomain.RemovePracticeExamDomainUseCase;
import br.com.naheroback.modules.instructor.useCases.updateStatus.UpdatePracticeExamStatusRequest;
import br.com.naheroback.modules.instructor.useCases.updateStatus.UpdatePracticeExamStatusUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/instructor")
public class InstructorController {
    private final ListInstructorExamsUseCase listInstructorExamsUseCase;
    private final CreateExamUseCase createExamUseCase;
    private final ListInstructorPracticeExamsUseCase listInstructorPracticeExamsUseCase;
    private final CreateInstructorPracticeExamUseCase createInstructorPracticeExamUseCase;
    private final GetInstructorPracticeExamUseCase getInstructorPracticeExamUseCase;
    private final UpdatePracticeExamStatusUseCase updatePracticeExamStatusUseCase;
    private final AddPracticeExamDomainsUseCase addPracticeExamDomainsUseCase;
    private final RemovePracticeExamDomainUseCase removePracticeExamDomainUseCase;
    private final ListInstructorQuestionsUseCase listInstructorQuestionsUseCase;
    private final CreateInstructorQuestionsUseCase createInstructorQuestionsUseCase;
    private final DeleteInstructorQuestionUseCase deleteInstructorQuestionUseCase;

    @GetMapping("/exams")
    public List<ListInstructorExamsResponse> listExams() {
        return listInstructorExamsUseCase.execute();
    }

    @PostMapping("/exams")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateExamResponse createExam(@Valid @RequestBody CreateExamRequest request) {
        return createExamUseCase.execute(request);
    }

    @GetMapping("/practice-exams")
    public List<ListInstructorPracticeExamsResponse> listPracticeExams() {
        return listInstructorPracticeExamsUseCase.execute();
    }

    @PostMapping("/practice-exams")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateInstructorPracticeExamResponse createPracticeExam(
            @Valid @RequestBody CreateInstructorPracticeExamRequest request) {
        return createInstructorPracticeExamUseCase.execute(request);
    }

    @GetMapping("/practice-exams/{practiceExamId}")
    public GetInstructorPracticeExamResponse getPracticeExam(@PathVariable @Positive Integer practiceExamId) {
        return getInstructorPracticeExamUseCase.execute(practiceExamId);
    }

    @PatchMapping("/practice-exams/{practiceExamId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStatus(@PathVariable @Positive Integer practiceExamId,
                             @Valid @RequestBody UpdatePracticeExamStatusRequest request) {
        updatePracticeExamStatusUseCase.execute(practiceExamId, request);
    }

    @PostMapping("/practice-exams/{practiceExamId}/domains")
    @ResponseStatus(HttpStatus.CREATED)
    public void addDomains(@PathVariable @Positive Integer practiceExamId,
                           @Valid @RequestBody AddPracticeExamDomainsRequest request) {
        addPracticeExamDomainsUseCase.execute(practiceExamId, request);
    }

    @DeleteMapping("/practice-exams/{practiceExamId}/domains/{domainId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeDomain(@PathVariable @Positive Integer practiceExamId, @PathVariable @Positive Integer domainId) {
        removePracticeExamDomainUseCase.execute(practiceExamId, domainId);
    }

    @GetMapping("/practice-exams/{practiceExamId}/questions")
    public Page<ListInstructorQuestionsResponse> listQuestions(@PathVariable @Positive Integer practiceExamId,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        return listInstructorQuestionsUseCase.execute(practiceExamId, page, size);
    }

    @PostMapping("/practice-exams/{practiceExamId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateInstructorQuestionsResponse createQuestions(@PathVariable @Positive Integer practiceExamId,
                                                             @Valid @RequestBody CreateInstructorQuestionsRequest request) {
        return createInstructorQuestionsUseCase.execute(practiceExamId, request);
    }

    @DeleteMapping("/practice-exams/{practiceExamId}/questions/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(@PathVariable @Positive Integer practiceExamId, @PathVariable @Positive Integer questionId) {
        deleteInstructorQuestionUseCase.execute(practiceExamId, questionId);
    }
}

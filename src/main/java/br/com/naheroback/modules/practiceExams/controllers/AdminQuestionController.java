package br.com.naheroback.modules.practiceExams.controllers;

import br.com.naheroback.modules.practiceExams.useCases.question.assignDomains.AssignQuestionDomainsRequest;
import br.com.naheroback.modules.practiceExams.useCases.question.assignDomains.AssignQuestionDomainsResponse;
import br.com.naheroback.modules.practiceExams.useCases.question.assignDomains.AssignQuestionDomainsUseCase;
import br.com.naheroback.modules.practiceExams.useCases.question.export.ExportQuestionsResponse;
import br.com.naheroback.modules.practiceExams.useCases.question.export.ExportQuestionsUseCase;
import br.com.naheroback.modules.practiceExams.useCases.question.listDomains.ListExamDomainsUseCase;
import br.com.naheroback.modules.practiceExams.services.ExamDomains;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/questions")
public class AdminQuestionController {

    private final AssignQuestionDomainsUseCase assignQuestionDomainsUseCase;
    private final ExportQuestionsUseCase exportQuestionsUseCase;
    private final ListExamDomainsUseCase listExamDomainsUseCase;

    @PutMapping("/domains")
    public AssignQuestionDomainsResponse assignDomains(@Valid @RequestBody List<AssignQuestionDomainsRequest> request) {
        return assignQuestionDomainsUseCase.execute(request);
    }

    @GetMapping("/domains")
    public Map<String, List<ExamDomains.Domain>> listDomains() {
        return listExamDomainsUseCase.execute();
    }

    @GetMapping("/export")
    public List<ExportQuestionsResponse> export(@RequestParam String practiceExamSlug) {
        return exportQuestionsUseCase.execute(practiceExamSlug);
    }
}

package br.com.naheroback.modules.practiceExams.useCases.questionBank.getFilters;

import java.util.List;

public record GetQuestionBankFiltersResponse(
        Integer practiceExamId,
        String slug,
        String title,
        List<DomainOption> domains
) {
    public record DomainOption(Integer id, String name) {}
}

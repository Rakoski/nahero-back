package br.com.naheroback.modules.practiceExams.useCases.questionBank.list;

import java.util.List;

public record ListQuestionBankResponse(
        List<Item> items,
        int page,
        int size,
        boolean hasNext
) {
    public record Item(
            Integer id,
            String preview,
            String type,
            boolean answered,
            PracticeExamSummary practiceExam,
            DomainSummary domain
    ) {}

    public record PracticeExamSummary(Integer id, String slug, String title) {}

    public record DomainSummary(Integer id, String name) {}
}

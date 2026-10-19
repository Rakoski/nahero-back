package br.com.naheroback.modules.questionLists.useCases.get;

import java.util.List;

public record GetQuestionListResponse(
        Integer id,
        String name,
        int questionCount,
        int answeredCount,
        List<Item> items,
        int page,
        int size,
        boolean hasNext
) {
    public record Item(
            Integer questionId,
            String preview,
            String type,
            boolean answered,
            PracticeExamSummary practiceExam,
            DomainSummary domain
    ) {}

    public record PracticeExamSummary(Integer id, String slug, String title) {}

    public record DomainSummary(Integer id, String name) {}
}

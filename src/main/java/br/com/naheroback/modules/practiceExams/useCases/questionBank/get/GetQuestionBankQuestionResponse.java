package br.com.naheroback.modules.practiceExams.useCases.questionBank.get;

import java.util.List;

public record GetQuestionBankQuestionResponse(
        Integer id,
        String content,
        String imageUrl,
        String type,
        boolean answered,
        PracticeExamSummary practiceExam,
        DomainSummary domain,
        List<AlternativeOption> alternatives
) {
    public record PracticeExamSummary(Integer id, String slug, String title) {}

    public record DomainSummary(Integer id, String name) {}

    public record AlternativeOption(Integer id, String content, String imageUrl) {}
}

package br.com.naheroback.modules.practiceExams.useCases.question.export;

import java.util.List;

public record ExportQuestionsResponse(
        Integer id,
        String practiceExamSlug,
        String language,
        String content,
        List<String> options,
        String domain
) {}

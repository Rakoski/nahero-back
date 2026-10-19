package br.com.naheroback.modules.instructor.useCases.listQuestions;

import java.time.LocalDateTime;

public record ListInstructorQuestionsResponse(
        Integer id,
        String preview,
        String type,
        String language,
        DomainSummary domain,
        long alternativeCount,
        long correctCount,
        LocalDateTime createdAt
) {
    public record DomainSummary(Integer id, String name) {
    }
}

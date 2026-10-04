package br.com.naheroback.modules.practiceExams.useCases.feedback.getFeedbackPage;

import br.com.naheroback.modules.practiceExams.services.AttemptDomainBreakdownService.DomainScore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GetFeedbackPageResponse(
        List<ExamOption> exams,
        ExamOption practiceExam,
        String weakestDomain,
        Integer latestAttemptId,
        List<AttemptSummary> attempts,
        List<DomainScore> domains,
        List<PracticeQuestion> practiceQuestions
) {
    public record ExamOption(String slug, String title) {}

    public record AttemptSummary(Integer attemptId, LocalDateTime endTime, Integer score, Integer total, Boolean passed) {}

    public record PracticeQuestion(Integer questionId, String type, String content, String imageUrl,
                                   List<PracticeAlternative> alternatives) {}

    public record PracticeAlternative(Integer alternativeId, String content, String imageUrl) {}

    public static GetFeedbackPageResponse empty() {
        return new GetFeedbackPageResponse(List.of(), null, null, null, null, null, null);
    }
}

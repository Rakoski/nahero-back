package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getFeedback;

import br.com.naheroback.modules.practiceExams.services.StudyPlan;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GetAttemptFeedbackResponse(String status, Boolean locked, String weakestDomain, StudyPlan content) {

    public static final String READY = "ready";
    public static final String PENDING = "pending";
    public static final String FAILED = "failed";

    public static GetAttemptFeedbackResponse locked(String weakestDomain) {
        return new GetAttemptFeedbackResponse(null, true, weakestDomain, null);
    }

    public static GetAttemptFeedbackResponse pending() {
        return new GetAttemptFeedbackResponse(PENDING, false, null, null);
    }

    public static GetAttemptFeedbackResponse failed() {
        return new GetAttemptFeedbackResponse(FAILED, false, null, null);
    }

    public static GetAttemptFeedbackResponse ready(StudyPlan content) {
        return new GetAttemptFeedbackResponse(READY, false, null, content);
    }

    public boolean isPending() {
        return PENDING.equals(status);
    }
}

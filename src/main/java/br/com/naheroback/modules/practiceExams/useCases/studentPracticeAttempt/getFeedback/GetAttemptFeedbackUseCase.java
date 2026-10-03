package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getFeedback;

import br.com.naheroback.modules.practiceExams.entities.AttemptFeedback;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.entities.enums.AttemptFeedbackStatus;
import br.com.naheroback.modules.practiceExams.entities.enums.PracticeAttemptStatusesEnum;
import br.com.naheroback.modules.practiceExams.repositories.AttemptFeedbackRepository;
import br.com.naheroback.modules.practiceExams.services.AttemptDomainBreakdownService;
import br.com.naheroback.modules.practiceExams.services.AttemptDomainBreakdownService.DomainScore;
import br.com.naheroback.modules.practiceExams.services.AttemptFeedbackService;
import br.com.naheroback.modules.practiceExams.services.PracticeAttemptEntitlementService;
import br.com.naheroback.modules.practiceExams.services.StudentAttemptAccessService;
import br.com.naheroback.modules.practiceExams.services.StudyPlan;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetAttemptFeedbackUseCase {
    private final StudentAttemptAccessService attemptAccess;
    private final PracticeAttemptEntitlementService entitlement;
    private final AttemptDomainBreakdownService domainBreakdownService;
    private final AttemptFeedbackRepository attemptFeedbackRepository;
    private final AttemptFeedbackService attemptFeedbackService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public GetAttemptFeedbackResponse execute(Integer attemptId) {
        StudentPracticeAttempt attempt = attemptAccess.loadOwnedAttempt(attemptId);

        if (!entitlement.canSeeStudyPlan()) {
            return GetAttemptFeedbackResponse.locked(
                    AttemptDomainBreakdownService.weakest(domainBreakdownService.breakdown(attempt))
                            .map(DomainScore::domain)
                            .orElse(null));
        }

        Optional<AttemptFeedback> stored = attemptFeedbackRepository.findByAttemptId(attemptId);
        if (stored.isPresent() && stored.get().getStatus() == AttemptFeedbackStatus.READY) {
            return GetAttemptFeedbackResponse.ready(parse(stored.get().getContent()));
        }

        if (stored.isPresent() && AttemptFeedbackService.isExhausted(stored.get())) return GetAttemptFeedbackResponse.failed();

        if (!Objects.equals(attempt.getAttemptStatus().getId(), PracticeAttemptStatusesEnum.IN_PROGRESS.getId())) {
            attemptFeedbackService.generate(attemptId);
        }

        return GetAttemptFeedbackResponse.pending();
    }

    private StudyPlan parse(String content) {
        try {
            return objectMapper.readValue(content, StudyPlan.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored study plan is not valid JSON", e);
        }
    }
}

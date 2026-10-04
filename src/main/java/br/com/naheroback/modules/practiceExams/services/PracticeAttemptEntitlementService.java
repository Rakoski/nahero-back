package br.com.naheroback.modules.practiceExams.services;

import br.com.naheroback.common.exceptions.custom.PaymentRequiredException;
import br.com.naheroback.modules.subscription.services.AccessChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PracticeAttemptEntitlementService {
    private final AccessChecker access;

    public void ensureCanSeeStudyFeedback() {
        if (access.isPremium()) return;

        throw new PaymentRequiredException("payment.dashboard_required");
    }

    public void ensureCanSeeHistory() {
        if (access.isPremium()) return;

        throw new PaymentRequiredException("payment.history_required");
    }

    public void ensureCanSeeFeedback() {
        if (access.isPremium()) return;

        throw new PaymentRequiredException("payment.feedback_required");
    }

    public boolean canSeeFeedback() {
        return access.isPremium();
    }

    public boolean canSeeExplanations() {
        return access.isPremium();
    }

    public boolean canSeeDomainBreakdown() {
        return access.isPremium();
    }
}

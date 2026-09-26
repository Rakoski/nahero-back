package br.com.naheroback.modules.practiceExams.services;

import br.com.naheroback.common.exceptions.custom.PaymentRequiredException;
import br.com.naheroback.modules.exams.entities.Exam;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.subscription.services.AccessChecker;
import br.com.naheroback.modules.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PracticeAttemptEntitlementService {
    private final UserRepository userRepository;
    private final AccessChecker access;

    public void ensureCanStart(Integer studentId, Exam exam) {
        if (!requiresEntitlement(exam) || access.isPremium()) return;

        int freeTriesLeft = userRepository.findFreeTriesLeftById(studentId).orElse(0);

        if (freeTriesLeft <= 0) throw new PaymentRequiredException("payment.required");
    }

    public void consumeFreeTryOnCompletion(StudentPracticeAttempt attempt) {
        if (!requiresEntitlement(attempt.getPracticeExam().getExam()) || access.isPremium()) return;

        Integer studentId = attempt.getEnrollment().getStudent().getId();

        userRepository.decrementFreeTriesIfAvailable(studentId);
    }

    private boolean requiresEntitlement(Exam exam) {
        Integer difficultyLevel = exam.getDifficultyLevel();
        return difficultyLevel != null && difficultyLevel > 1;
    }
}

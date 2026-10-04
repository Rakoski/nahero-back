package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getResult;

import br.com.naheroback.modules.practiceExams.entities.StudentAnswer;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.repositories.StudentAnswerRepository;
import br.com.naheroback.modules.practiceExams.services.AttemptDomainBreakdownService;
import br.com.naheroback.modules.practiceExams.services.PracticeAttemptEntitlementService;
import br.com.naheroback.modules.practiceExams.services.StudentAttemptAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetResultUseCase {
    private final StudentAttemptAccessService attemptAccess;
    private final StudentAnswerRepository studentAnswerRepository;
    private final AttemptDomainBreakdownService domainBreakdownService;
    private final PracticeAttemptEntitlementService entitlement;

    @Transactional(readOnly = true)
    public GetResultResponse execute(Integer studentPracticeAttemptId) {
        StudentPracticeAttempt attempt = attemptAccess.loadOwnedAttempt(studentPracticeAttemptId);

        List<StudentAnswer> answers = studentAnswerRepository.findAllByStudentPracticeAttemptId(studentPracticeAttemptId);

        GetResultResponse response = GetResultResponse.toPresentation(attempt, answers, domainBreakdownService.domainsOf(answers));

        if (!entitlement.canSeeDomainBreakdown()) {
            response.setDomains(null);
            response.setWeakestDomain(null);
            response.setQuestions(response.getQuestions().stream()
                    .map(question -> new GetResultResponse.QuestionResult(question.questionId(), null, question.correct()))
                    .toList());
        }

        return response;
    }
}

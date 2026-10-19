package br.com.naheroback.modules.practiceExams.useCases.questionBank.getFilters;

import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.entities.PracticeExamDomain;
import br.com.naheroback.modules.practiceExams.repositories.PracticeExamDomainRepository;
import br.com.naheroback.modules.practiceExams.services.PracticeAttemptEntitlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetQuestionBankFiltersUseCase {
    private final PracticeExamDomainRepository practiceExamDomainRepository;
    private final PracticeAttemptEntitlementService entitlement;

    @Transactional(readOnly = true)
    public List<GetQuestionBankFiltersResponse> execute() {
        entitlement.ensureCanUseQuestionBank();

        Map<PracticeExam, List<PracticeExamDomain>> linksByExam = practiceExamDomainRepository
                .findAllWithPracticeExamAndDomain().stream()
                .filter(link -> !Boolean.FALSE.equals(link.getPracticeExam().getIsActive()))
                .collect(Collectors.groupingBy(PracticeExamDomain::getPracticeExam, LinkedHashMap::new, Collectors.toList()));

        return linksByExam.entrySet().stream()
                .map(entry -> new GetQuestionBankFiltersResponse(
                        entry.getKey().getId(),
                        entry.getKey().getSlug(),
                        entry.getKey().getTitle(),
                        entry.getValue().stream()
                                .map(link -> new GetQuestionBankFiltersResponse.DomainOption(
                                        link.getDomain().getId(), link.getDomain().getName()))
                                .toList()))
                .toList();
    }
}

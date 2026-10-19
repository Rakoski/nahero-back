package br.com.naheroback.modules.practiceExams.useCases.question.listDomains;

import br.com.naheroback.modules.practiceExams.repositories.PracticeExamDomainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListExamDomainsUseCase {

    private final PracticeExamDomainRepository practiceExamDomainRepository;

    @Secured("IS_ADMIN")
    @Transactional(readOnly = true)
    public Map<String, List<ListExamDomainsResponse>> execute() {
        return practiceExamDomainRepository.findAllWithPracticeExamAndDomain().stream()
                .collect(Collectors.groupingBy(
                        link -> link.getPracticeExam().getSlug(),
                        TreeMap::new,
                        Collectors.mapping(
                                link -> new ListExamDomainsResponse(
                                        link.getDomain().getId(),
                                        link.getDomain().getName(),
                                        link.getMinWeight(),
                                        link.getMaxWeight()),
                                Collectors.toList())));
    }
}

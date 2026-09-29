package br.com.naheroback.modules.practiceExams.useCases.question.listDomains;

import br.com.naheroback.modules.practiceExams.services.ExamDomains;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListExamDomainsUseCase {

    private final ExamDomains examDomains;

    @Secured("IS_ADMIN")
    public Map<String, List<ExamDomains.Domain>> execute() {
        return examDomains.mappedPracticeExamSlugs().stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        examDomains::forPracticeExam,
                        (first, second) -> first,
                        TreeMap::new));
    }
}

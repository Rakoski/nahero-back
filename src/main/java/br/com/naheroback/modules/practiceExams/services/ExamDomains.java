package br.com.naheroback.modules.practiceExams.services;

import br.com.naheroback.modules.practiceExams.entities.Domain;
import br.com.naheroback.modules.practiceExams.entities.PracticeExamDomain;
import br.com.naheroback.modules.practiceExams.repositories.PracticeExamDomainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExamDomains {

    private final PracticeExamDomainRepository practiceExamDomainRepository;

    public record Catalog(Map<Integer, Map<String, Domain>> byPracticeExamId) {
        public boolean hasPracticeExam(Integer practiceExamId) {
            return practiceExamId != null && byPracticeExamId.containsKey(practiceExamId);
        }

        public Optional<Domain> canonical(Integer practiceExamId, String candidate) {
            if (candidate == null || !hasPracticeExam(practiceExamId)) {
                return Optional.empty();
            }
            return Optional.ofNullable(byPracticeExamId.get(practiceExamId).get(normalise(candidate)));
        }
    }

    public Catalog forPracticeExams(Collection<Integer> practiceExamIds) {
        if (practiceExamIds == null || practiceExamIds.isEmpty()) {
            return new Catalog(Map.of());
        }

        return new Catalog(practiceExamDomainRepository.findAllByPracticeExamIdIn(practiceExamIds).stream()
                .collect(Collectors.groupingBy(
                        link -> link.getPracticeExam().getId(),
                        Collectors.toMap(
                                link -> normalise(link.getDomain().getName()),
                                PracticeExamDomain::getDomain,
                                (first, second) -> first))));
    }

    private static String normalise(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}

package br.com.naheroback.modules.practiceExams.services;

import br.com.naheroback.common.exceptions.custom.UnprocessableEntityException;
import br.com.naheroback.modules.practiceExams.entities.Domain;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.entities.PracticeExamDomain;
import br.com.naheroback.modules.practiceExams.repositories.DomainRepository;
import br.com.naheroback.modules.practiceExams.repositories.PracticeExamDomainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PracticeExamDomainService {
    private final DomainRepository domainRepository;
    private final PracticeExamDomainRepository practiceExamDomainRepository;

    public record DomainSpec(String name, Double minWeight, Double maxWeight) {
        String key() {
            return name.trim().toLowerCase(Locale.ROOT);
        }
    }

    public List<PracticeExamDomain> link(PracticeExam practiceExam, List<DomainSpec> specs) {
        if (specs == null || specs.isEmpty()) return List.of();

        rejectDuplicates(specs);
        rejectInvertedWeights(specs);
        rejectAlreadyLinked(practiceExam, specs);

        return practiceExamDomainRepository.saveAll(specs.stream()
                .map(spec -> toLink(practiceExam, spec))
                .toList());
    }

    private PracticeExamDomain toLink(PracticeExam practiceExam, DomainSpec spec) {
        Domain domain = domainRepository.findByNameIgnoreCase(spec.name())
                .orElseGet(() -> {
                    Domain created = new Domain();
                    created.setName(spec.name().trim());
                    return domainRepository.save(created);
                });

        PracticeExamDomain link = new PracticeExamDomain();
        link.setPracticeExam(practiceExam);
        link.setDomain(domain);
        link.setMinWeight(spec.minWeight());
        link.setMaxWeight(spec.maxWeight() != null ? spec.maxWeight() : spec.minWeight());
        return link;
    }

    private void rejectDuplicates(List<DomainSpec> specs) {
        List<String> duplicates = specs.stream()
                .collect(Collectors.groupingBy(DomainSpec::key, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        if (!duplicates.isEmpty()) {
            throw new UnprocessableEntityException("practiceexam.domain.duplicate", String.join(", ", duplicates));
        }
    }

    private void rejectInvertedWeights(List<DomainSpec> specs) {
        List<String> inverted = specs.stream()
                .filter(spec -> spec.minWeight() != null && spec.maxWeight() != null
                        && spec.minWeight() > spec.maxWeight())
                .map(spec -> spec.name().trim())
                .toList();
        if (!inverted.isEmpty()) {
            throw new UnprocessableEntityException("practiceexam.domain.weight_range", String.join(", ", inverted));
        }
    }

    private void rejectAlreadyLinked(PracticeExam practiceExam, List<DomainSpec> specs) {
        if (practiceExam.getId() == null) return;

        Set<String> linked = practiceExamDomainRepository.findAllByPracticeExamIdIn(List.of(practiceExam.getId()))
                .stream()
                .map(link -> link.getDomain().getName().trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        List<String> clashes = specs.stream()
                .filter(spec -> linked.contains(spec.key()))
                .map(spec -> spec.name().trim())
                .toList();
        if (!clashes.isEmpty()) {
            throw new UnprocessableEntityException("practiceexam.domain.already_linked", String.join(", ", clashes));
        }
    }
}

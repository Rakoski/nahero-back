package br.com.naheroback.modules.instructor.useCases.addDomains;

import br.com.naheroback.modules.practiceExams.services.PracticeExamDomainService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AddPracticeExamDomainsRequest(
        @NotEmpty(message = "{instructor.domains.required}") @Size(max = 30)
        List<@Valid @NotNull DomainRequest> domains
) {
    public record DomainRequest(
            @NotBlank(message = "{practiceexam.domain.name.required}") @Size(max = 160) String name,
            @PositiveOrZero @DecimalMax("100") Double minWeight,
            @PositiveOrZero @DecimalMax("100") Double maxWeight
    ) {
        public PracticeExamDomainService.DomainSpec toSpec() {
            return new PracticeExamDomainService.DomainSpec(name, minWeight, maxWeight);
        }
    }
}

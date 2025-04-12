package br.com.naheroback.modules.practiceExams.useCases.alternative.create;

import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practiceExams.entities.Alternative;
import br.com.naheroback.modules.practiceExams.repositories.AlternativeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CreateAlternativeUseCase {
    private final AlternativeRepository alternativeRepository;

    @Transactional
    @Secured("IS_TEACHER")
    public Integer execute(CreateAlternativeRequest request) {
        int teacherId = AuthService.getUserFromToken().getId();
        int version = 0;

        if (Objects.nonNull(request.baseAlternativeId()))
            version = alternativeRepository.findVersionByBaseAlternativeId(request.baseAlternativeId());

        Alternative alternative = CreateAlternativeRequest.toDomain(request, teacherId, version + 1);
        alternativeRepository.save(alternative);
        return alternative.getId();
    }

}

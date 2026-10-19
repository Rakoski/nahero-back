package br.com.naheroback.modules.instructor.useCases.removeDomain;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.common.exceptions.custom.UnprocessableEntityException;
import br.com.naheroback.modules.instructor.services.InstructorAccess;
import br.com.naheroback.modules.practiceExams.entities.Domain;
import br.com.naheroback.modules.practiceExams.entities.PracticeExamDomain;
import br.com.naheroback.modules.practiceExams.repositories.PracticeExamDomainRepository;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RemovePracticeExamDomainUseCase {
    private final InstructorAccess instructorAccess;
    private final PracticeExamDomainRepository practiceExamDomainRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    @Secured("IS_TEACHER")
    public void execute(Integer practiceExamId, Integer domainId) {
        instructorAccess.ownedPracticeExam(practiceExamId);

        PracticeExamDomain link = practiceExamDomainRepository.findByPracticeExamIdAndDomainId(practiceExamId, domainId).orElseThrow(() -> NotFoundException.with(Domain.class, "id", domainId));
        if (questionRepository.existsByPracticeExamIdAndDomainId(practiceExamId, domainId)) {
            throw new UnprocessableEntityException("instructor.domain.in_use");
        }

        practiceExamDomainRepository.delete(link);
    }
}

package br.com.naheroback.modules.instructor.useCases.addDomains;

import br.com.naheroback.modules.instructor.services.InstructorAccess;
import br.com.naheroback.modules.practiceExams.services.PracticeExamDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddPracticeExamDomainsUseCase {
    private final InstructorAccess instructorAccess;
    private final PracticeExamDomainService practiceExamDomainService;

    @Transactional
    @Secured("IS_TEACHER")
    public void execute(Integer practiceExamId, AddPracticeExamDomainsRequest request) {
        practiceExamDomainService.link(
                instructorAccess.ownedPracticeExam(practiceExamId),
                request.domains().stream().map(AddPracticeExamDomainsRequest.DomainRequest::toSpec).toList());
    }
}

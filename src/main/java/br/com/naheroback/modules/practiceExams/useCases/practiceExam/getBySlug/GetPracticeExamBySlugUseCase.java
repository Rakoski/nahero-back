package br.com.naheroback.modules.practiceExams.useCases.practiceExam.getBySlug;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.repositories.PracticeExamRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetPracticeExamBySlugUseCase {
    private final PracticeExamRepository practiceExamRepository;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public GetPracticeExamBySlugResponse execute(String slug) {
        PracticeExam practiceExam = practiceExamRepository.findBySlug(slug)
                .orElseThrow(() -> NotFoundException.with(PracticeExam.class, "slug", slug));

        return modelMapper.map(practiceExam, GetPracticeExamBySlugResponse.class);
    }
}

package br.com.naheroback.modules.practice_exams.useCases.practiceExams.list;

import br.com.naheroback.modules.practice_exams.repositories.PracticeExamRepository;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListPracticeExamsUseCase {
    private final PracticeExamRepository practiceExamRepository;
    private final ListPracticeExamsResponse listPracticeExamsResponse;

    @Transactional(readOnly = true)
    public Page<ListPracticeExamsResponse> execute(Predicate predicate, Pageable pagination) {
        return practiceExamRepository.findAll(predicate, pagination).map(listPracticeExamsResponse::toPresentation);
    }
}

package br.com.naheroback.modules.practiceExams.useCases.practiceExam.list;

import br.com.naheroback.modules.practiceExams.repositories.PracticeExamRepository;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListPracticeExamsUseCase {
    private final PracticeExamRepository practiceExamRepository;
    private final ListPracticeExamsResponse listPracticeExamsResponse;

    @Transactional(readOnly = true)
    public Page<ListPracticeExamsResponse> execute(Predicate predicate, Pageable pagination) {
        Pageable sortedPageable = PageRequest.of(pagination.getPageNumber(), pagination.getPageSize(),
                Sort.by(Sort.Order.asc("difficultyLevel")));

        return practiceExamRepository.findAll(predicate, sortedPageable).map(listPracticeExamsResponse::toPresentation);
    }
}

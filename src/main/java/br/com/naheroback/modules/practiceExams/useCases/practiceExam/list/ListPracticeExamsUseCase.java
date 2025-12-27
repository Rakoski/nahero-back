package br.com.naheroback.modules.practiceExams.useCases.practiceExam.list;

import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.entities.QPracticeExam;
import br.com.naheroback.modules.practiceExams.repositories.PracticeExamRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper; // Import ModelMapper
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ListPracticeExamsUseCase {

    private final PracticeExamRepository practiceExamRepository;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public Page<ListPracticeExamsResponse> execute(
            ListPracticeExamsRequest request,
            Predicate predicate,
            Pageable pagination
    ) {
        Pageable sortedPageable = PageRequest.of(pagination.getPageNumber(), pagination.getPageSize(),
                Sort.by(Sort.Order.asc("timeLimit")));

        BooleanBuilder builder = new BooleanBuilder();
        if (predicate != null) builder.and(predicate);

        addRequestFilters(builder, request);

        return practiceExamRepository.findAll(builder, sortedPageable).map(this::toPresentation);
    }

    private ListPracticeExamsResponse toPresentation(PracticeExam practiceExam) {
        return modelMapper.map(practiceExam, ListPracticeExamsResponse.class);
    }

    private void addRequestFilters(BooleanBuilder builder, ListPracticeExamsRequest request) {
        QPracticeExam qPracticeExam = QPracticeExam.practiceExam;

        if (Objects.nonNull(request.search()) && !request.search().isBlank()) {
            builder.and(
                    qPracticeExam.title.containsIgnoreCase(request.search())
                            .or(qPracticeExam.description.containsIgnoreCase(request.search()))
            );
        }

        if (Objects.nonNull(request.category()) && !request.category().isBlank()) {
            builder.and(qPracticeExam.exam.category.containsIgnoreCase(request.category()));
        }

        if (Objects.nonNull(request.difficultyLevel())) {
            builder.and(qPracticeExam.exam.difficultyLevel.eq(Integer.parseInt(request.difficultyLevel())));
        }
    }
}
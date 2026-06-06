package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getHistory;

import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practiceExams.entities.QStudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GetHistoryUseCase {
    private final StudentPracticeAttemptRepository studentPracticeAttemptRepository;

    @Transactional(readOnly = true)
    public Page<GetHistoryResponse> execute(GetHistoryFilterDTO filter, Pageable pageable) {
        Integer studentId = AuthService.getUserFromToken().getId();

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("id"))
        );

        BooleanBuilder builder = new BooleanBuilder();
        addFilters(builder, filter, studentId);

        Page<StudentPracticeAttempt> attempts = studentPracticeAttemptRepository.findAll(builder, sortedPageable);

        return attempts.map(GetHistoryResponse::toPresentation);
    }

    private void addFilters(BooleanBuilder builder, GetHistoryFilterDTO filter, Integer studentId) {
        QStudentPracticeAttempt qAttempt = QStudentPracticeAttempt.studentPracticeAttempt;

        builder.and(qAttempt.enrollment.student.id.eq(studentId));

        if (Objects.nonNull(filter.getPracticeExamId())) {
            builder.and(qAttempt.practiceExam.id.eq(filter.getPracticeExamId()));
        }

        if (Objects.nonNull(filter.getStartDate())) {
            builder.and(qAttempt.endTime.goe(filter.getStartDate()));
        }

        if (Objects.nonNull(filter.getEndDate())) {
            builder.and(qAttempt.endTime.loe(filter.getEndDate()));
        }

        if (Objects.nonNull(filter.getScore())) {
            builder.and(qAttempt.score.eq(filter.getScore()));
        }
    }
}

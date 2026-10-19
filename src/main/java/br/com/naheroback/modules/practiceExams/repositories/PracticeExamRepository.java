package br.com.naheroback.modules.practiceExams.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.common.utils.StringUtils;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PracticeExamRepository extends BaseRepository<PracticeExam, Integer> {
    Optional<PracticeExam> findBySlug(String slug);
    Optional<PracticeExam> findBySlugAndIsActiveTrue(String slug);
    Optional<PracticeExam> findByIdAndIsActiveTrue(Integer id);
    List<PracticeExam> findAllByIdIn(List<Integer> ids);
    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = "exam")
    List<PracticeExam> findAllByTeacherIdOrderByCreatedAtDesc(Integer teacherId);

    @Query("""
            SELECT pe.exam.id, COUNT(pe) FROM PracticeExam pe
            WHERE pe.exam.id IN :examIds
            GROUP BY pe.exam.id
            """)
    List<Object[]> countByExamIds(@Param("examIds") Collection<Integer> examIds);

    default String uniqueSlug(String title) {
        String base = StringUtils.createSlug(title);
        String candidate = base;
        int suffix = 2;
        while (existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}

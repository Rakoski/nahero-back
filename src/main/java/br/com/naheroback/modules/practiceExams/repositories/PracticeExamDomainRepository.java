package br.com.naheroback.modules.practiceExams.repositories;

import br.com.naheroback.modules.practiceExams.entities.PracticeExamDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RepositoryRestResource(exported = false)
public interface PracticeExamDomainRepository extends JpaRepository<PracticeExamDomain, Integer> {

    @Query("""
            SELECT ped FROM PracticeExamDomain ped
            JOIN FETCH ped.practiceExam pe
            JOIN FETCH ped.domain d
            ORDER BY pe.slug ASC, ped.id ASC
            """)
    List<PracticeExamDomain> findAllWithPracticeExamAndDomain();

    @Query("""
            SELECT ped FROM PracticeExamDomain ped
            JOIN FETCH ped.domain d
            WHERE ped.practiceExam.id IN :practiceExamIds
            ORDER BY ped.id ASC
            """)
    List<PracticeExamDomain> findAllByPracticeExamIdIn(@Param("practiceExamIds") Collection<Integer> practiceExamIds);

    Optional<PracticeExamDomain> findByPracticeExamIdAndDomainId(Integer practiceExamId, Integer domainId);
}

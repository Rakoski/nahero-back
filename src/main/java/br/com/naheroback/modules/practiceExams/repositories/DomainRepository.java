package br.com.naheroback.modules.practiceExams.repositories;

import br.com.naheroback.modules.practiceExams.entities.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(exported = false)
public interface DomainRepository extends JpaRepository<Domain, Integer> {

    @Query("SELECT d FROM Domain d WHERE LOWER(d.name) = LOWER(TRIM(:name))")
    Optional<Domain> findByNameIgnoreCase(@Param("name") String name);
}

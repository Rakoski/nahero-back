package br.com.naheroback.modules.practiceExams;

import br.com.naheroback.common.exceptions.custom.BadRequestException;
import br.com.naheroback.modules.auth.entities.AuthenticatedUser;
import br.com.naheroback.modules.practiceExams.useCases.question.assignDomains.AssignQuestionDomainsRequest;
import br.com.naheroback.modules.practiceExams.useCases.question.assignDomains.AssignQuestionDomainsUseCase;
import br.com.naheroback.modules.user.entities.Role;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.entities.enums.RolesEnum;
import br.com.naheroback.modules.user.repositories.RoleRepository;
import br.com.naheroback.modules.user.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "reengagement.enabled=false",
        "payment.reconcile.enabled=false"
})
class DomainSchemaIntegrationTest {

    private static final String BACKFILL = "db/migration/V36__backfill_domains_from_catalog_and_questions.sql";
    private static final String CATALOG_SLUG = "aws-cloud-practitioner-clf-02";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private AssignQuestionDomainsUseCase assignQuestionDomains;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private final List<Integer> examIds = new ArrayList<>();
    private final List<Integer> practiceExamIds = new ArrayList<>();
    private Integer adminId;

    @BeforeEach
    void setUp() {
        Role admin = roleRepository.findByName(RolesEnum.IS_ADMIN.name()).orElseThrow();
        User user = new User();
        user.setName("Domain Admin");
        user.setEmail("domain-admin-%s@example.com".formatted(System.nanoTime()));
        user.setPassword("irrelevant");
        user.setRoles(new HashSet<>(Set.of(admin)));
        User saved = userRepository.save(user);
        adminId = saved.getId();

        AuthenticatedUser principal = new AuthenticatedUser(saved);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        for (Integer id : practiceExamIds) {
            jdbcTemplate.update("DELETE FROM questions WHERE practice_exam_id = ?", id);
            DomainFixtures.unlinkAll(jdbcTemplate, id);
            jdbcTemplate.update("DELETE FROM practice_exams WHERE id = ?", id);
        }
        examIds.forEach(id -> jdbcTemplate.update("DELETE FROM exams WHERE id = ?", id));
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", adminId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", adminId);
    }

    @Test
    @DisplayName("Should link catalog domains with weights and backfill question domain_id from the legacy strings")
    void shouldBackfillFromCatalogAndLegacyStrings() throws IOException {
        Integer practiceExamId = insertPracticeExam(CATALOG_SLUG);
        Integer canonical = insertLegacyQuestion(practiceExamId, "Cloud Concepts");
        Integer sloppy = insertLegacyQuestion(practiceExamId, "  security and compliance ");
        Integer offCatalog = insertLegacyQuestion(practiceExamId, "Legacy Only Domain");
        Integer none = insertLegacyQuestion(practiceExamId, null);

        runBackfill();

        List<Map<String, Object>> links = jdbcTemplate.queryForList("""
                SELECT d.name, ped.min_weight, ped.max_weight FROM practice_exam_domains ped
                JOIN domains d ON d.id = ped.domain_id
                WHERE ped.practice_exam_id = ?
                ORDER BY d.name
                """, practiceExamId);
        assertEquals(List.of(
                        "Billing, Pricing, and Support",
                        "Cloud Concepts",
                        "Cloud Technology and Services",
                        "Legacy Only Domain",
                        "Security and Compliance"),
                links.stream().map(link -> link.get("name")).toList());

        Map<String, Object> concepts = links.get(1);
        assertEquals(0, new BigDecimal("24").compareTo((BigDecimal) concepts.get("min_weight")));
        assertEquals(0, new BigDecimal("24").compareTo((BigDecimal) concepts.get("max_weight")));
        assertNull(links.get(3).get("min_weight"));

        assertEquals("Cloud Concepts", domainNameOf(canonical));
        assertEquals("Security and Compliance", domainNameOf(sloppy));
        assertEquals("Legacy Only Domain", domainNameOf(offCatalog));
        assertNull(domainNameOf(none));
    }

    @Test
    @DisplayName("Should change nothing when the backfill runs a second time")
    void shouldBeIdempotent() throws IOException {
        Integer practiceExamId = insertPracticeExam(CATALOG_SLUG);
        insertLegacyQuestion(practiceExamId, "Cloud Concepts");

        runBackfill();
        Integer domains = jdbcTemplate.queryForObject("SELECT count(*) FROM domains", Integer.class);
        Integer links = jdbcTemplate.queryForObject("SELECT count(*) FROM practice_exam_domains", Integer.class);

        runBackfill();

        assertEquals(domains, jdbcTemplate.queryForObject("SELECT count(*) FROM domains", Integer.class));
        assertEquals(links, jdbcTemplate.queryForObject("SELECT count(*) FROM practice_exam_domains", Integer.class));
    }

    @Test
    @DisplayName("Should reject a question whose domain is not one of its practice exam's domains")
    void shouldRejectDomainFromAnotherPracticeExam() {
        Integer first = insertPracticeExam("domain-fk-first-" + System.nanoTime());
        Integer second = insertPracticeExam("domain-fk-second-" + System.nanoTime());
        Integer foreignDomainId = DomainFixtures.link(jdbcTemplate, second, "Only On Second " + System.nanoTime());

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO questions (practice_exam_id, question_type_id, content, points, version, is_active, language, domain_id)
                VALUES (?, 3, 'Question', 1, 1, true, 'en', ?)
                """, first, foreignDomainId));
    }

    @Test
    @DisplayName("Should assign the practice exam's canonical domain regardless of case and spacing")
    void shouldAssignCanonicalDomain() {
        Integer practiceExamId = insertPracticeExam("domain-assign-" + System.nanoTime());
        Integer domainId = DomainFixtures.link(jdbcTemplate, practiceExamId, "Cloud Concepts");
        Integer questionId = insertLegacyQuestion(practiceExamId, null);

        assignQuestionDomains.execute(List.of(new AssignQuestionDomainsRequest(questionId, "  cloud CONCEPTS ")));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT domain_id, domain FROM questions WHERE id = ?", questionId);
        assertEquals(domainId, row.get("domain_id"));
        assertEquals("Cloud Concepts", row.get("domain"));
    }

    @Test
    @DisplayName("Should refuse to assign a domain the practice exam does not have")
    void shouldRefuseForeignDomain() {
        Integer practiceExamId = insertPracticeExam("domain-refuse-" + System.nanoTime());
        Integer other = insertPracticeExam("domain-refuse-other-" + System.nanoTime());
        DomainFixtures.link(jdbcTemplate, practiceExamId, "Cloud Concepts");
        DomainFixtures.link(jdbcTemplate, other, "Security and Compliance");
        Integer questionId = insertLegacyQuestion(practiceExamId, null);

        assertThrows(BadRequestException.class, () -> assignQuestionDomains.execute(
                List.of(new AssignQuestionDomainsRequest(questionId, "Security and Compliance"))));
        assertNull(jdbcTemplate.queryForObject("SELECT domain_id FROM questions WHERE id = ?", Integer.class, questionId));
    }

    @Test
    @DisplayName("Should stamp updated_at on real changes made straight in the database and skip no-op updates")
    void shouldStampUpdatedAtFromTrigger() {
        Integer practiceExamId = insertPracticeExam("domain-updated-at-" + System.nanoTime());
        String name = "Updated At " + System.nanoTime();
        Integer domainId = jdbcTemplate.queryForObject(
                "INSERT INTO domains (name, updated_at) VALUES (?, '2020-01-01') RETURNING id", Integer.class, name);
        jdbcTemplate.update("""
                INSERT INTO practice_exam_domains (practice_exam_id, domain_id, updated_at) VALUES (?, ?, '2020-01-01')
                """, practiceExamId, domainId);

        jdbcTemplate.update("UPDATE domains SET name = name WHERE id = ?", domainId);
        assertEquals(LocalDateTime.of(2020, 1, 1, 0, 0), domainUpdatedAt(domainId));

        jdbcTemplate.update("UPDATE domains SET name = ? WHERE id = ?", name + " Renamed", domainId);
        assertTrue(domainUpdatedAt(domainId).getYear() > 2020);

        jdbcTemplate.update("UPDATE practice_exam_domains SET min_weight = 12.5 WHERE practice_exam_id = ?", practiceExamId);
        assertTrue(jdbcTemplate.queryForObject(
                "SELECT updated_at FROM practice_exam_domains WHERE practice_exam_id = ?", LocalDateTime.class, practiceExamId)
                .getYear() > 2020);

        DomainFixtures.unlinkAll(jdbcTemplate, practiceExamId);
        jdbcTemplate.update("DELETE FROM domains WHERE id = ?", domainId);
    }

    private LocalDateTime domainUpdatedAt(Integer domainId) {
        return jdbcTemplate.queryForObject("SELECT updated_at FROM domains WHERE id = ?", LocalDateTime.class, domainId);
    }

    private void runBackfill() throws IOException {
        String sql = new ClassPathResource(BACKFILL).getContentAsString(StandardCharsets.UTF_8);
        transactionTemplate.executeWithoutResult(status -> jdbcTemplate.execute(sql));
    }

    private Integer insertPracticeExam(String slug) {
        Integer examId = jdbcTemplate.queryForObject("""
                INSERT INTO exams (title, difficulty_level, is_active) VALUES (?, 2, true) RETURNING id
                """, Integer.class, slug + " Exam");
        examIds.add(examId);
        Integer id = jdbcTemplate.queryForObject("""
                INSERT INTO practice_exams (exam_id, title, slug, passing_score, time_limit, number_of_questions, is_active)
                VALUES (?, ?, ?, 50, 60, 2, true) RETURNING id
                """, Integer.class, examId, slug, slug);
        practiceExamIds.add(id);
        return id;
    }

    private Integer insertLegacyQuestion(Integer practiceExamId, String domain) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO questions (practice_exam_id, question_type_id, content, points, version, is_active, language, domain)
                VALUES (?, 3, 'Question', 1, 1, true, 'en', ?) RETURNING id
                """, Integer.class, practiceExamId, domain);
    }

    private String domainNameOf(Integer questionId) {
        return jdbcTemplate.queryForObject("""
                SELECT d.name FROM questions q LEFT JOIN domains d ON d.id = q.domain_id WHERE q.id = ?
                """, String.class, questionId);
    }
}

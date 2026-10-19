package br.com.naheroback.modules.practiceExams;

import org.springframework.jdbc.core.JdbcTemplate;

public final class DomainFixtures {

    private DomainFixtures() {
    }

    public static Integer link(JdbcTemplate jdbcTemplate, Integer practiceExamId, String name) {
        if (name == null) return null;

        jdbcTemplate.update("""
                INSERT INTO domains (name)
                SELECT ? WHERE NOT EXISTS (SELECT 1 FROM domains WHERE LOWER(name) = LOWER(?))
                """, name, name);
        Integer domainId = jdbcTemplate.queryForObject(
                "SELECT id FROM domains WHERE LOWER(name) = LOWER(?)", Integer.class, name);
        jdbcTemplate.update("""
                INSERT INTO practice_exam_domains (practice_exam_id, domain_id) VALUES (?, ?)
                ON CONFLICT (practice_exam_id, domain_id) DO NOTHING
                """, practiceExamId, domainId);
        return domainId;
    }

    public static void unlinkAll(JdbcTemplate jdbcTemplate, Integer practiceExamId) {
        jdbcTemplate.update("DELETE FROM practice_exam_domains WHERE practice_exam_id = ?", practiceExamId);
    }
}

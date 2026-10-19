CREATE TABLE domains
(
    id SERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_domains_name_lower ON domains (LOWER(name));

CREATE TABLE practice_exam_domains
(
    id SERIAL PRIMARY KEY,
    practice_exam_id INTEGER NOT NULL REFERENCES practice_exams (id),
    domain_id INTEGER NOT NULL REFERENCES domains (id),
    min_weight NUMERIC(5, 2) NULL,
    max_weight NUMERIC(5, 2) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_practice_exam_domains_exam_domain UNIQUE (practice_exam_id, domain_id)
);

CREATE INDEX idx_practice_exam_domains_domain ON practice_exam_domains (domain_id);

ALTER TABLE questions ADD COLUMN domain_id INTEGER NULL REFERENCES domains (id);

CREATE INDEX idx_questions_practice_exam_domain_id ON questions (practice_exam_id, domain_id);
CREATE INDEX idx_questions_domain_id ON questions (domain_id);

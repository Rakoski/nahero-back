ALTER TABLE questions ADD COLUMN domain VARCHAR(160);

CREATE INDEX idx_questions_practice_exam_domain ON questions (practice_exam_id, domain);

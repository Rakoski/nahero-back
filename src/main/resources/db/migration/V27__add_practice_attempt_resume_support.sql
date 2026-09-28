ALTER TABLE student_practice_attempts ADD COLUMN last_question_index INTEGER;

CREATE TABLE student_attempt_answer_drafts
(
    id SERIAL PRIMARY KEY,
    student_practice_attempt_id INTEGER NOT NULL,
    question_id INTEGER NOT NULL,
    selected_alternative_ids TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_answer_drafts_attempt FOREIGN KEY (student_practice_attempt_id)
        REFERENCES student_practice_attempts (id),
    CONSTRAINT fk_answer_drafts_question FOREIGN KEY (question_id)
        REFERENCES questions (id)
);

CREATE INDEX idx_answer_drafts_attempt ON student_attempt_answer_drafts (student_practice_attempt_id);

CREATE UNIQUE INDEX uq_answer_drafts_attempt_question
    ON student_attempt_answer_drafts (student_practice_attempt_id, question_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_student_practice_attempts_status ON student_practice_attempts (status);

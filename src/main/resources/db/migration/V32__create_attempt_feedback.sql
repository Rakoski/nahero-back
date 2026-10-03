CREATE TABLE attempt_feedback
(
    id SERIAL PRIMARY KEY,
    attempt_id INTEGER NOT NULL REFERENCES student_practice_attempts (id),
    language VARCHAR(10) NOT NULL,
    model VARCHAR(64) NOT NULL,
    content JSONB NOT NULL,
    prompt_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_attempt_feedback_attempt UNIQUE (attempt_id)
);

CREATE TABLE question_study_answers
(
    id SERIAL PRIMARY KEY,
    student_id INTEGER NOT NULL REFERENCES users (id),
    question_id INTEGER NOT NULL REFERENCES questions (id),
    question_version INTEGER NOT NULL,
    selected_alternative_ids TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_question_study_answers_student_question ON question_study_answers (student_id, question_id);

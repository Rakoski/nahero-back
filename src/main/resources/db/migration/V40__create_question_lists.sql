CREATE TABLE question_lists
(
    id SERIAL PRIMARY KEY,
    student_id INTEGER NOT NULL REFERENCES users (id),
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL
);

CREATE INDEX idx_question_lists_student ON question_lists (student_id);

CREATE TABLE question_list_items
(
    id SERIAL PRIMARY KEY,
    question_list_id INTEGER NOT NULL REFERENCES question_lists (id),
    question_id INTEGER NOT NULL REFERENCES questions (id),
    position INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_question_list_items_list_question UNIQUE (question_list_id, question_id)
);

CREATE INDEX idx_question_list_items_list_position ON question_list_items (question_list_id, position);

ALTER TABLE question_study_answers ADD COLUMN question_list_id INTEGER NULL REFERENCES question_lists (id);

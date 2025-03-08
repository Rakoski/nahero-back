CREATE TABLE exams (
     id SERIAL PRIMARY KEY,
     title VARCHAR(255) NOT NULL,
     description TEXT,
     teacher_id INTEGER REFERENCES users(id),
     category VARCHAR(100),
     is_active BOOLEAN DEFAULT true,
     difficulty_level INTEGER, -- scale from 0 to 10, 0 very easy and 10 being very hard
     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP NULL,
     deleted_at TIMESTAMP NULL
);

CREATE TABLE enrollment_statuses (
     id SERIAL PRIMARY KEY,
     name VARCHAR(50) NOT NULL,
     description TEXT,
     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP NULL,
     deleted_at TIMESTAMP NULL,
     UNIQUE(name)
);

INSERT INTO enrollment_statuses (name, description) VALUES
     ('active', 'Student is currently enrolled and can access all materials'),
     ('completed', 'Student has successfully completed the exam'),
     ('expired', 'Student''s enrollment period has ended'),
     ('suspended', 'Enrollment temporarily suspended'),
     ('cancelled', 'Enrollment cancelled before completion');


CREATE TABLE enrollments (
     id SERIAL PRIMARY KEY,
     student_id INTEGER REFERENCES users(id),
     exam_id INTEGER REFERENCES exams(id),
     enrollment_status_id INTEGER REFERENCES enrollment_statuses(id),
     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP NULL,
     deleted_at TIMESTAMP NULL,
     UNIQUE(student_id, exam_id)
);

CREATE TABLE practice_exams (
    id SERIAL PRIMARY KEY,
    exam_id INTEGER REFERENCES exams(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    passing_score INTEGER,
    teacher_id INTEGER REFERENCES users(id),
    time_limit INTEGER, -- in minutes
    difficulty_level INTEGER, -- scale from 0 to 10, 0 very easy and 10 being very hard
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL
);

CREATE TABLE question_types (
     id SERIAL PRIMARY KEY,
     name VARCHAR(50) NOT NULL,
     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP NULL,
     deleted_at TIMESTAMP NULL,
     UNIQUE(name)
);

INSERT INTO question_types (name) VALUES
     ('multiple_choice'),
     ('true_false'),
     ('objective'),
     ('descriptive'),
     ('sum');

CREATE TABLE questions (
     id SERIAL PRIMARY KEY,
     base_question_id INTEGER, -- self-reference to track versions of the same question
     practice_exam_id INTEGER REFERENCES practice_exams(id),
     question_type_id INTEGER REFERENCES question_types(id),
     content TEXT NOT NULL,
     image_url VARCHAR(255),
     explanation TEXT,
     points INTEGER DEFAULT 1,
     version INTEGER NOT NULL DEFAULT 1,
     is_active BOOLEAN DEFAULT true,
     created_by INTEGER REFERENCES users(id),
     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP NULL,
     deleted_at TIMESTAMP NULL
);

ALTER TABLE questions
    ADD CONSTRAINT fk_base_question
        FOREIGN KEY (base_question_id) REFERENCES questions(id);

ALTER TABLE questions ADD CONSTRAINT unique_question_version UNIQUE (id, version);

CREATE INDEX idx_questions_version ON questions(base_question_id, version);

CREATE TABLE alternatives (
      id SERIAL PRIMARY KEY,
      base_alternative_id INTEGER,
      question_id INTEGER REFERENCES questions(id),
      image_url VARCHAR(255),
      content TEXT NOT NULL,
      is_correct BOOLEAN NOT NULL,
      version INTEGER NOT NULL DEFAULT 1,
      is_active BOOLEAN DEFAULT true,
      created_by INTEGER REFERENCES users(id),
      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP NULL,
      deleted_at TIMESTAMP NULL
);

ALTER TABLE alternatives
    ADD CONSTRAINT fk_base_alternative
        FOREIGN KEY (base_alternative_id) REFERENCES alternatives(id);

ALTER TABLE alternatives ADD CONSTRAINT unique_alternative_version UNIQUE (id, version);

CREATE INDEX idx_alternatives_version ON alternatives(base_alternative_id, version);

CREATE TABLE practice_attempt_statuses (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    UNIQUE(name)
);

INSERT INTO practice_attempt_statuses (name, description) VALUES
    ('in_progress', 'Student is currently taking the practice exam'),
    ('completed', 'Student has submitted all answers and completed the practice exam'),
    ('abandoned', 'Student started but did not complete the practice exam'),
    ('timed_out', 'Practice exam was automatically submitted after time expired');


CREATE TABLE student_practice_attempts (
     id SERIAL PRIMARY KEY,
     enrollment_id INTEGER REFERENCES enrollments(id),
     practice_exam_id INTEGER REFERENCES practice_exams(id),
     status INTEGER REFERENCES practice_attempt_statuses(id),
     start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     end_time TIMESTAMP NULL,
     score INTEGER,
     passed BOOLEAN,
     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP NULL,
     deleted_at TIMESTAMP NULL
);

CREATE TABLE student_answers (
     id SERIAL PRIMARY KEY,
     student_practice_attempt_id INTEGER REFERENCES student_practice_attempts(id),
     question_id INTEGER NOT NULL,
     question_version INTEGER NOT NULL,
     selected_alternative_id INTEGER,
     selected_alternative_version INTEGER,
     is_correct BOOLEAN,
     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP NULL,
     deleted_at TIMESTAMP NULL,

     CONSTRAINT fk_question_version FOREIGN KEY (question_id, question_version)
         REFERENCES questions(id, version),

     CONSTRAINT fk_alternative_version FOREIGN KEY (selected_alternative_id, selected_alternative_version)
         REFERENCES alternatives(id, version)
);

CREATE INDEX idx_enrollments_student ON enrollments(student_id);
CREATE INDEX idx_enrollments_exam ON enrollments(exam_id);
CREATE INDEX idx_practice_exams_exam ON practice_exams(exam_id);
CREATE INDEX idx_questions_practice_exam ON questions(practice_exam_id);
CREATE INDEX idx_alternatives_question ON alternatives(question_id);
CREATE INDEX idx_student_practice_attempts_enrollment ON student_practice_attempts(enrollment_id);
CREATE INDEX idx_student_practice_attempts_practice_exam ON student_practice_attempts(practice_exam_id);
CREATE INDEX idx_student_answers_attempt ON student_answers(student_practice_attempt_id);
CREATE INDEX idx_student_answers_question ON student_answers(question_id, question_version);
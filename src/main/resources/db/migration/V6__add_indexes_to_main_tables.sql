-- Add indexes to users table
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_cpf ON users(cpf);
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users(deleted_at);
CREATE INDEX IF NOT EXISTS idx_users_email_confirmed_at ON users(email_confirmed_at);
CREATE INDEX IF NOT EXISTS idx_users_external_customer_id ON users(external_customer_id);
CREATE INDEX IF NOT EXISTS idx_users_address_id ON users(address_id);

-- Add indexes to exams table
CREATE INDEX IF NOT EXISTS idx_exams_teacher_id ON exams(teacher_id);
CREATE INDEX IF NOT EXISTS idx_exams_category ON exams(category);
CREATE INDEX IF NOT EXISTS idx_exams_is_active ON exams(is_active);
CREATE INDEX IF NOT EXISTS idx_exams_deleted_at ON exams(deleted_at);
CREATE INDEX IF NOT EXISTS idx_exams_title ON exams(title);

-- Add indexes to practice_exams table
CREATE INDEX IF NOT EXISTS idx_practice_exams_exam_id ON practice_exams(exam_id);
CREATE INDEX IF NOT EXISTS idx_practice_exams_teacher_id ON practice_exams(teacher_id);
CREATE INDEX IF NOT EXISTS idx_practice_exams_is_active ON practice_exams(is_active);
CREATE INDEX IF NOT EXISTS idx_practice_exams_deleted_at ON practice_exams(deleted_at);
CREATE INDEX IF NOT EXISTS idx_practice_exams_title ON practice_exams(title);
CREATE INDEX IF NOT EXISTS idx_practice_exams_difficulty_level ON practice_exams(difficulty_level);

-- Add indexes to questions table
CREATE INDEX IF NOT EXISTS idx_questions_practice_exam_id ON questions(practice_exam_id);
CREATE INDEX IF NOT EXISTS idx_questions_question_type_id ON questions(question_type_id);
CREATE INDEX IF NOT EXISTS idx_questions_base_question_id ON questions(base_question_id);
CREATE INDEX IF NOT EXISTS idx_questions_teacher_id ON questions(teacher_id);
CREATE INDEX IF NOT EXISTS idx_questions_is_active ON questions(is_active);
CREATE INDEX IF NOT EXISTS idx_questions_deleted_at ON questions(deleted_at);
CREATE INDEX IF NOT EXISTS idx_questions_version ON questions(version);

-- Add indexes to alternatives table
CREATE INDEX IF NOT EXISTS idx_alternatives_question_id ON alternatives(question_id);
CREATE INDEX IF NOT EXISTS idx_alternatives_base_alternative_id ON alternatives(base_alternative_id);
CREATE INDEX IF NOT EXISTS idx_alternatives_teacher_id ON alternatives(teacher_id);
CREATE INDEX IF NOT EXISTS idx_alternatives_is_correct ON alternatives(is_correct);
CREATE INDEX IF NOT EXISTS idx_alternatives_is_active ON alternatives(is_active);
CREATE INDEX IF NOT EXISTS idx_alternatives_deleted_at ON alternatives(deleted_at);
CREATE INDEX IF NOT EXISTS idx_alternatives_version ON alternatives(version);


-- Add column to store the shuffled question IDs for each student attempt
-- This ensures consistent question order across pagination requests
ALTER TABLE student_practice_attempts
ADD COLUMN shuffled_question_ids TEXT;

COMMENT ON COLUMN student_practice_attempts.shuffled_question_ids IS 'Comma-separated list of question IDs in randomized order for this attempt';


UPDATE exams SET difficulty_level = 1 WHERE difficulty_level IS NULL;

ALTER TABLE exams ALTER COLUMN difficulty_level SET NOT NULL;

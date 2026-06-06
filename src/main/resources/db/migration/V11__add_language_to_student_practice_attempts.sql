ALTER TABLE student_practice_attempts ADD COLUMN language VARCHAR(10);

UPDATE student_practice_attempts SET language = 'en' WHERE language IS NULL;

ALTER TABLE student_practice_attempts ALTER COLUMN language SET NOT NULL;

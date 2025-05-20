UPDATE practice_exams
SET passing_score = 70
WHERE passing_score IS NULL;

ALTER TABLE practice_exams
    ALTER COLUMN passing_score SET NOT NULL;
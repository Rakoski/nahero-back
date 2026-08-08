CREATE EXTENSION IF NOT EXISTS unaccent;

ALTER TABLE practice_exams ADD COLUMN slug VARCHAR(255);

UPDATE practice_exams
SET slug = trim(both '-' from regexp_replace(
    regexp_replace(lower(unaccent(title)), '[^a-z0-9]+', '-', 'g'),
    '-+', '-', 'g'
));

WITH dups AS (
    SELECT id,
           slug,
           ROW_NUMBER() OVER (PARTITION BY slug ORDER BY id) AS rn
    FROM practice_exams
)
UPDATE practice_exams p
SET slug = p.slug || '-' || d.rn
FROM dups d
WHERE p.id = d.id AND d.rn > 1;

ALTER TABLE practice_exams ALTER COLUMN slug SET NOT NULL;

CREATE UNIQUE INDEX idx_practice_exams_slug ON practice_exams (slug);

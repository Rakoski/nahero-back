ALTER TABLE domains ADD COLUMN updated_at TIMESTAMP NULL;
ALTER TABLE practice_exam_domains ADD COLUMN updated_at TIMESTAMP NULL;

UPDATE domains SET updated_at = created_at WHERE updated_at IS NULL;
UPDATE practice_exam_domains SET updated_at = created_at WHERE updated_at IS NULL;

ALTER TABLE domains ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE practice_exam_domains ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_domains_updated_at
    BEFORE UPDATE ON domains
    FOR EACH ROW
    WHEN (OLD.* IS DISTINCT FROM NEW.*)
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_practice_exam_domains_updated_at
    BEFORE UPDATE ON practice_exam_domains
    FOR EACH ROW
    WHEN (OLD.* IS DISTINCT FROM NEW.*)
EXECUTE FUNCTION set_updated_at();

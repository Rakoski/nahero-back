ALTER TABLE reengagement_emails ADD COLUMN status VARCHAR(32) NULL;
ALTER TABLE reengagement_emails ADD COLUMN failure_reason TEXT NULL;

UPDATE reengagement_emails SET status = 'SENT' WHERE status IS NULL;

CREATE INDEX idx_reengagement_emails_status ON reengagement_emails (status);

CREATE TABLE reengagement_emails
(
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    email_type VARCHAR(64) NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    campaign_started_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_reengagement_emails_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_reengagement_emails_user ON reengagement_emails (user_id);
CREATE INDEX idx_reengagement_emails_sent_at ON reengagement_emails (sent_at);

ALTER TABLE users ADD COLUMN reengagement_opted_out_at TIMESTAMP NULL;
ALTER TABLE users ADD COLUMN reengagement_unsubscribe_token VARCHAR(255) NULL;

CREATE INDEX idx_users_reengagement_unsubscribe_token ON users (reengagement_unsubscribe_token)
    WHERE reengagement_unsubscribe_token IS NOT NULL;

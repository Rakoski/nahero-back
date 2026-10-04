CREATE TABLE announcement_emails
(
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users (id),
    campaign VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    failure_reason TEXT NULL,
    sent_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_announcement_emails_user_campaign UNIQUE (user_id, campaign)
);

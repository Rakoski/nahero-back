CREATE TABLE email_outbox
(
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    email_type VARCHAR(64) NOT NULL,
    locale VARCHAR(35) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL,
    last_error TEXT NULL,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_email_outbox_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_email_outbox_user ON email_outbox (user_id);
CREATE INDEX idx_email_outbox_pending ON email_outbox (next_attempt_at) WHERE status = 'PENDING';

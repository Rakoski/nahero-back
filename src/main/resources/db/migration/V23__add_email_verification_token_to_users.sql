ALTER TABLE users ADD COLUMN email_verification_token VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN email_verification_token_expires_at TIMESTAMP NULL;

CREATE INDEX idx_users_email_verification_token ON users (email_verification_token)
    WHERE email_verification_token IS NOT NULL;

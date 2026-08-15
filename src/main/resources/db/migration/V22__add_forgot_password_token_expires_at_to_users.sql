ALTER TABLE users ADD COLUMN forgot_password_token_expires_at TIMESTAMP NULL;

CREATE INDEX idx_users_forgot_password_token ON users (forgot_password_token)
    WHERE forgot_password_token IS NOT NULL;

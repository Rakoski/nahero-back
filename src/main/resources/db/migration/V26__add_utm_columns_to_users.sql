-- First-touch acquisition tags, written once at sign-up and never updated.
-- Organic sign-ups leave these null.
ALTER TABLE users ADD COLUMN utm_source VARCHAR(64) NULL;
ALTER TABLE users ADD COLUMN utm_medium VARCHAR(64) NULL;
ALTER TABLE users ADD COLUMN utm_campaign VARCHAR(64) NULL;

CREATE INDEX idx_users_utm_source ON users (utm_source) WHERE utm_source IS NOT NULL;

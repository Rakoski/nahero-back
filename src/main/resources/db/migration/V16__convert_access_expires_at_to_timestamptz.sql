ALTER TABLE users
    ALTER COLUMN access_expires_at TYPE TIMESTAMP WITH TIME ZONE
    USING access_expires_at AT TIME ZONE 'UTC';

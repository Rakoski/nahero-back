ALTER TABLE users ADD COLUMN stripe_customer_id VARCHAR(255) NULL;

CREATE INDEX idx_users_stripe_customer_id ON users (stripe_customer_id);
CREATE INDEX idx_users_stripe_subscription_id ON users (stripe_subscription_id);

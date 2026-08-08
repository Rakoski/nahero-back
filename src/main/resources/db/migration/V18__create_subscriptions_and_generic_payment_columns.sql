-- Provider-agnostic payment identity on users.
ALTER TABLE users ADD COLUMN payment_provider VARCHAR(32);

-- Backfill external_customer_id from the old stripe_customer_id where it wasn't already populated.
UPDATE users
   SET external_customer_id = stripe_customer_id
 WHERE external_customer_id IS NULL
   AND stripe_customer_id IS NOT NULL;

UPDATE users
   SET payment_provider = 'STRIPE'
 WHERE external_customer_id IS NOT NULL;

CREATE INDEX idx_users_payment_provider ON users (payment_provider);

-- Move subscription identity + access expiry off users into its own aggregate.
CREATE TABLE subscriptions (
    id                       SERIAL PRIMARY KEY,
    user_id                  INTEGER NOT NULL REFERENCES users (id),
    provider                 VARCHAR(32) NOT NULL,
    external_subscription_id VARCHAR(255) NOT NULL,
    status                   VARCHAR(32) NOT NULL,
    current_period_end       TIMESTAMP WITH TIME ZONE,
    created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP,
    deleted_at               TIMESTAMP,
    CONSTRAINT uq_subscriptions_provider_external UNIQUE (provider, external_subscription_id)
);

CREATE INDEX idx_subscriptions_user_id ON subscriptions (user_id);
CREATE INDEX idx_subscriptions_current_period_end ON subscriptions (current_period_end);
CREATE INDEX idx_subscriptions_deleted_at ON subscriptions (deleted_at);

-- Migrate any existing subscription state out of users.
INSERT INTO subscriptions (user_id, provider, external_subscription_id, status, current_period_end)
SELECT id,
       'STRIPE',
       stripe_subscription_id,
       CASE WHEN access_expires_at IS NOT NULL AND access_expires_at > CURRENT_TIMESTAMP
            THEN 'ACTIVE'
            ELSE 'CANCELED'
       END,
       access_expires_at
  FROM users
 WHERE stripe_subscription_id IS NOT NULL;

-- Drop the Stripe-specific columns now that the data has moved.
DROP INDEX IF EXISTS idx_users_stripe_customer_id;
DROP INDEX IF EXISTS idx_users_stripe_subscription_id;

ALTER TABLE users DROP COLUMN stripe_customer_id;
ALTER TABLE users DROP COLUMN stripe_subscription_id;
ALTER TABLE users DROP COLUMN access_expires_at;

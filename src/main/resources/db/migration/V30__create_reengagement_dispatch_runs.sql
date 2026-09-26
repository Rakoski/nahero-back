CREATE TABLE reengagement_dispatch_runs
(
    id SERIAL PRIMARY KEY,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP NOT NULL,
    duration_ms BIGINT NOT NULL,
    candidates INTEGER NOT NULL,
    sent INTEGER NOT NULL,
    failed INTEGER NOT NULL,
    skipped INTEGER NOT NULL,
    funnel JSONB NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL
);

CREATE INDEX idx_reengagement_dispatch_runs_started_at ON reengagement_dispatch_runs (started_at);

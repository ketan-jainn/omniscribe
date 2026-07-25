CREATE TABLE users (
    id VARCHAR(128) PRIMARY KEY,
    plan VARCHAR(32) NOT NULL DEFAULT 'free',
    rate_limit_tier VARCHAR(32) NOT NULL DEFAULT 'default',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE jobs (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    s3_prefix VARCHAR(1024),
    chunk_count INTEGER,
    idempotency_key VARCHAR(255) UNIQUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX ix_jobs_user_id ON jobs (user_id);
CREATE INDEX ix_jobs_idempotency_key ON jobs (idempotency_key);

CREATE TABLE chunks (
    id VARCHAR(36) PRIMARY KEY,
    job_id VARCHAR(36) NOT NULL,
    "index" INTEGER NOT NULL,
    s3_key VARCHAR(1024) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_chunks_job_index UNIQUE (job_id, "index"),
    CONSTRAINT fk_chunks_job_id FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE
);

CREATE INDEX ix_chunks_job_id ON chunks (job_id);
CREATE INDEX ix_chunks_job_index ON chunks (job_id, "index");

CREATE TABLE segments (
    id VARCHAR(36) PRIMARY KEY,
    job_id VARCHAR(36) NOT NULL,
    chunk_index INTEGER NOT NULL,
    seq INTEGER NOT NULL,
    start_ms INTEGER,
    end_ms INTEGER,
    text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_segments_job_chunk_seq UNIQUE (job_id, chunk_index, seq),
    CONSTRAINT fk_segments_job_id FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE
);

CREATE INDEX ix_segments_job_id ON segments (job_id);
CREATE INDEX ix_segments_job_chunk ON segments (job_id, chunk_index);
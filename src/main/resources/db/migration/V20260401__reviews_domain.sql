-- Product reviews / ratings domain (one review per account for MVP).
-- NOTE: Flyway may not be enabled in all environments yet.
-- Keep this script for controlled/manual schema setup where needed.

CREATE TABLE IF NOT EXISTS review (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT,
    is_deleted BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    account_id BIGINT NOT NULL,
    rating INTEGER NOT NULL,
    title VARCHAR(120),
    comment_text VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    display_name_snapshot VARCHAR(150) NOT NULL,
    moderated_by_account_id BIGINT,
    moderated_at TIMESTAMP,
    moderation_note VARCHAR(500),
    CONSTRAINT uk_review_account UNIQUE (account_id),
    CONSTRAINT fk_review_account FOREIGN KEY (account_id) REFERENCES account(id),
    CONSTRAINT chk_review_rating_range CHECK (rating >= 1 AND rating <= 5)
);

CREATE INDEX IF NOT EXISTS idx_review_status ON review (status);
CREATE INDEX IF NOT EXISTS idx_review_featured ON review (featured);
CREATE INDEX IF NOT EXISTS idx_review_updated_at ON review (updated_at);


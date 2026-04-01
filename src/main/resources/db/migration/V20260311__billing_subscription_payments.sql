-- Billing/Subscription schema for PayOS + VNPay integration
-- NOTE: Flyway is not currently enabled in pom.xml. Run this script manually
-- on PostgreSQL environments that should be pre-seeded before app startup.

CREATE TABLE IF NOT EXISTS subscription_plan (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT,
    is_deleted BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    plan_code VARCHAR(60) NOT NULL,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    amount BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    duration_days INTEGER NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_subscription_plan_code UNIQUE (plan_code)
);

CREATE INDEX IF NOT EXISTS idx_subscription_plan_active ON subscription_plan (is_active);
CREATE INDEX IF NOT EXISTS idx_subscription_plan_sort ON subscription_plan (sort_order);

CREATE TABLE IF NOT EXISTS payment_order (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT,
    is_deleted BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    order_code VARCHAR(80) NOT NULL,
    account_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    gateway VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    gateway_order_ref VARCHAR(120),
    gateway_transaction_ref VARCHAR(160),
    checkout_url VARCHAR(1000),
    raw_init_payload TEXT,
    paid_at TIMESTAMP,
    expires_at TIMESTAMP,
    CONSTRAINT uk_payment_order_code UNIQUE (order_code),
    CONSTRAINT fk_payment_order_plan FOREIGN KEY (plan_id) REFERENCES subscription_plan(id)
);

CREATE INDEX IF NOT EXISTS idx_payment_order_account_created ON payment_order (account_id, created_at);
CREATE INDEX IF NOT EXISTS idx_payment_order_status ON payment_order (status);
CREATE INDEX IF NOT EXISTS idx_payment_order_gateway_ref ON payment_order (gateway_order_ref);
CREATE INDEX IF NOT EXISTS idx_payment_order_gateway_txn ON payment_order (gateway_transaction_ref);

CREATE TABLE IF NOT EXISTS payment_event (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT,
    is_deleted BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    payment_order_id BIGINT,
    gateway VARCHAR(20) NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    event_hash VARCHAR(80) NOT NULL,
    gateway_event_ref VARCHAR(160),
    signature VARCHAR(600),
    signature_valid BOOLEAN NOT NULL DEFAULT FALSE,
    raw_payload TEXT,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processing_result VARCHAR(260),
    processed_at TIMESTAMP,
    CONSTRAINT uk_payment_event_hash UNIQUE (event_hash),
    CONSTRAINT fk_payment_event_order FOREIGN KEY (payment_order_id) REFERENCES payment_order(id)
);

CREATE INDEX IF NOT EXISTS idx_payment_event_order ON payment_event (payment_order_id);
CREATE INDEX IF NOT EXISTS idx_payment_event_gateway_type ON payment_event (gateway, event_type);
CREATE INDEX IF NOT EXISTS idx_payment_event_created ON payment_event (created_at);

CREATE TABLE IF NOT EXISTS user_subscription (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT,
    is_deleted BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    account_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    last_payment_order_code VARCHAR(80),
    CONSTRAINT uk_user_subscription_account UNIQUE (account_id),
    CONSTRAINT fk_user_subscription_plan FOREIGN KEY (plan_id) REFERENCES subscription_plan(id)
);

CREATE INDEX IF NOT EXISTS idx_user_subscription_status ON user_subscription (status);
CREATE INDEX IF NOT EXISTS idx_user_subscription_end ON user_subscription (ends_at);

INSERT INTO subscription_plan (
    plan_code, title, description, amount, currency, duration_days, sort_order, is_active, is_deleted
)
SELECT 'PREMIUM_MONTHLY_BASIC', 'Premium Basic', '30 ngay premium co ban', 79000, 'VND', 30, 1, TRUE, FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM subscription_plan WHERE plan_code = 'PREMIUM_MONTHLY_BASIC'
);

INSERT INTO subscription_plan (
    plan_code, title, description, amount, currency, duration_days, sort_order, is_active, is_deleted
)
SELECT 'PREMIUM_MONTHLY_PRO', 'Premium Pro', '30 ngay premium day du', 129000, 'VND', 30, 2, TRUE, FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM subscription_plan WHERE plan_code = 'PREMIUM_MONTHLY_PRO'
);

INSERT INTO subscription_plan (
    plan_code, title, description, amount, currency, duration_days, sort_order, is_active, is_deleted
)
SELECT 'PREMIUM_YEARLY_PRO', 'Premium Pro Yearly', '365 ngay premium tiet kiem hon', 1199000, 'VND', 365, 3, TRUE, FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM subscription_plan WHERE plan_code = 'PREMIUM_YEARLY_PRO'
);

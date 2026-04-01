-- Entitlements + push token foundation + achievement seed (idempotent)

CREATE TABLE IF NOT EXISTS notification_device_token (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT,
    is_deleted BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    account_id BIGINT NOT NULL,
    device_token VARCHAR(600) NOT NULL,
    platform VARCHAR(32),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notification_device_token_account_token UNIQUE (account_id, device_token)
);

CREATE INDEX IF NOT EXISTS idx_notification_device_token_account ON notification_device_token (account_id);
CREATE INDEX IF NOT EXISTS idx_notification_device_token_enabled ON notification_device_token (enabled);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_name = 'achievement'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM achievement LIMIT 1) THEN
            INSERT INTO achievement (
                code,
                name,
                description,
                icon,
                tier,
                category,
                xp_reward,
                target_value,
                is_hidden,
                is_active,
                sort_order,
                is_deleted,
                created_at,
                updated_at
            )
            VALUES
                ('SURVEY_FIRST_COMPLETE', 'Hoan tat khao sat dau tien', 'Hoan tat khao sat hanh vi lan dau', 'survey', 'BRONZE', 'MILESTONES', 20, 1, FALSE, TRUE, 1, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                ('FIRST_TRANSACTION', 'Giao dich dau tien', 'Ghi nhan giao dich dau tien cua ban', 'tx', 'BRONZE', 'TRANSACTIONS', 15, 1, FALSE, TRUE, 2, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                ('FIRST_INCOME', 'Thu nhap dau tien', 'Ghi nhan giao dich thu nhap dau tien', 'income', 'BRONZE', 'TRANSACTIONS', 15, 1, FALSE, TRUE, 3, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                ('FIRST_EXPENSE', 'Chi tieu dau tien', 'Ghi nhan giao dich chi tieu dau tien', 'expense', 'BRONZE', 'TRANSACTIONS', 15, 1, FALSE, TRUE, 4, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                ('STREAK_7_DAYS', '7 ngay lien tiep', 'Duy tri chuoi hoat dong 7 ngay lien tiep', 'streak', 'SILVER', 'STREAKS', 40, 7, FALSE, TRUE, 5, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                ('SAVE_100K', 'Tiet kiem 100000', 'Dat moc tiet kiem 100000 VND', 'save100k', 'SILVER', 'SAVINGS', 35, 100000, FALSE, TRUE, 6, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                ('SAVE_1M', 'Tiet kiem 1000000', 'Dat moc tiet kiem 1000000 VND', 'save1m', 'GOLD', 'SAVINGS', 80, 1000000, FALSE, TRUE, 7, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                ('FIRST_GOAL', 'Muc tieu dau tien', 'Tao muc tieu tiet kiem dau tien', 'goal', 'BRONZE', 'SAVINGS', 20, 1, FALSE, TRUE, 8, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                ('PET_LEVEL_3', 'Pet len cap 3', 'Dat cap 3 trong he thong gamification', 'pet3', 'SILVER', 'PET', 45, 3, FALSE, TRUE, 9, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                ('FIRST_SUBSCRIPTION_UPGRADE', 'Nang cap goi dau tien', 'Nang cap Plus/Premium lan dau', 'upgrade', 'GOLD', 'MILESTONES', 60, 1, FALSE, TRUE, 10, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
        END IF;
    END IF;
END $$;

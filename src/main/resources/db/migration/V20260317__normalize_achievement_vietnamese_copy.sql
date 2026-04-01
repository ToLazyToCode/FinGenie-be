-- Normalize achievement seed copy to Vietnamese with full diacritics.
-- Idempotent: safe to run multiple times.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'achievement'
    ) THEN
        UPDATE achievement
        SET name = 'Hoàn tất khảo sát đầu tiên',
            description = 'Hoàn tất khảo sát hành vi lần đầu',
            updated_at = CURRENT_TIMESTAMP
        WHERE code = 'SURVEY_FIRST_COMPLETE'
          AND (
              name IS DISTINCT FROM 'Hoàn tất khảo sát đầu tiên'
              OR description IS DISTINCT FROM 'Hoàn tất khảo sát hành vi lần đầu'
          );

        UPDATE achievement
        SET name = 'Giao dịch đầu tiên',
            description = 'Ghi nhận giao dịch đầu tiên của bạn',
            updated_at = CURRENT_TIMESTAMP
        WHERE code = 'FIRST_TRANSACTION'
          AND (
              name IS DISTINCT FROM 'Giao dịch đầu tiên'
              OR description IS DISTINCT FROM 'Ghi nhận giao dịch đầu tiên của bạn'
          );

        UPDATE achievement
        SET name = 'Thu nhập đầu tiên',
            description = 'Ghi nhận giao dịch thu nhập đầu tiên',
            updated_at = CURRENT_TIMESTAMP
        WHERE code = 'FIRST_INCOME'
          AND (
              name IS DISTINCT FROM 'Thu nhập đầu tiên'
              OR description IS DISTINCT FROM 'Ghi nhận giao dịch thu nhập đầu tiên'
          );

        UPDATE achievement
        SET name = 'Chi tiêu đầu tiên',
            description = 'Ghi nhận giao dịch chi tiêu đầu tiên',
            updated_at = CURRENT_TIMESTAMP
        WHERE code = 'FIRST_EXPENSE'
          AND (
              name IS DISTINCT FROM 'Chi tiêu đầu tiên'
              OR description IS DISTINCT FROM 'Ghi nhận giao dịch chi tiêu đầu tiên'
          );

        UPDATE achievement
        SET name = 'Hoàn thành 7 ngày liên tiếp',
            description = 'Duy trì chuỗi hoạt động 7 ngày liên tiếp',
            updated_at = CURRENT_TIMESTAMP
        WHERE code = 'STREAK_7_DAYS'
          AND (
              name IS DISTINCT FROM 'Hoàn thành 7 ngày liên tiếp'
              OR description IS DISTINCT FROM 'Duy trì chuỗi hoạt động 7 ngày liên tiếp'
          );

        UPDATE achievement
        SET name = 'Tiết kiệm 100.000đ',
            description = 'Đạt mốc tiết kiệm 100.000đ',
            updated_at = CURRENT_TIMESTAMP
        WHERE code = 'SAVE_100K'
          AND (
              name IS DISTINCT FROM 'Tiết kiệm 100.000đ'
              OR description IS DISTINCT FROM 'Đạt mốc tiết kiệm 100.000đ'
          );

        UPDATE achievement
        SET name = 'Tiết kiệm 1.000.000đ',
            description = 'Đạt mốc tiết kiệm 1.000.000đ',
            updated_at = CURRENT_TIMESTAMP
        WHERE code = 'SAVE_1M'
          AND (
              name IS DISTINCT FROM 'Tiết kiệm 1.000.000đ'
              OR description IS DISTINCT FROM 'Đạt mốc tiết kiệm 1.000.000đ'
          );

        UPDATE achievement
        SET name = 'Mục tiêu đầu tiên',
            description = 'Tạo mục tiêu tiết kiệm đầu tiên',
            updated_at = CURRENT_TIMESTAMP
        WHERE code = 'FIRST_GOAL'
          AND (
              name IS DISTINCT FROM 'Mục tiêu đầu tiên'
              OR description IS DISTINCT FROM 'Tạo mục tiêu tiết kiệm đầu tiên'
          );

        UPDATE achievement
        SET name = 'Pet đạt cấp 3',
            description = 'Nâng cấp pet đạt cấp độ 3',
            updated_at = CURRENT_TIMESTAMP
        WHERE code = 'PET_LEVEL_3'
          AND (
              name IS DISTINCT FROM 'Pet đạt cấp 3'
              OR description IS DISTINCT FROM 'Nâng cấp pet đạt cấp độ 3'
          );

        UPDATE achievement
        SET name = 'Nâng cấp gói lần đầu',
            description = 'Nâng cấp Plus hoặc Premium lần đầu',
            updated_at = CURRENT_TIMESTAMP
        WHERE code = 'FIRST_SUBSCRIPTION_UPGRADE'
          AND (
              name IS DISTINCT FROM 'Nâng cấp gói lần đầu'
              OR description IS DISTINCT FROM 'Nâng cấp Plus hoặc Premium lần đầu'
          );
    END IF;
END $$;

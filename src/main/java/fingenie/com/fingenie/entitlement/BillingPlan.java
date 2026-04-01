package fingenie.com.fingenie.entitlement;

import java.util.Locale;

public enum BillingPlan {
    FREE,
    PLUS_MONTHLY,
    PREMIUM_MONTHLY,
    PREMIUM_YEARLY;

    public static BillingPlan fromPlanCode(String planCode) {
        if (planCode == null || planCode.isBlank()) {
            return FREE;
        }

        String normalized = planCode.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PREMIUM_MONTHLY_BASIC" -> PLUS_MONTHLY;
            case "PREMIUM_MONTHLY_PRO" -> PREMIUM_MONTHLY;
            case "PREMIUM_YEARLY_PRO" -> PREMIUM_YEARLY;
            default -> FREE;
        };
    }
}

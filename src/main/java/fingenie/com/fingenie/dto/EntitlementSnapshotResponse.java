package fingenie.com.fingenie.dto;

import fingenie.com.fingenie.entitlement.BillingPlan;
import fingenie.com.fingenie.entitlement.PlanTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntitlementSnapshotResponse {
    private PlanTier planTier;
    private BillingPlan billingPlan;
    private Map<String, Object> entitlements;
    private Map<String, Boolean> features;
    private Map<String, Long> limits;
    private Map<String, Long> usage;
    private Map<String, Long> remaining;
}

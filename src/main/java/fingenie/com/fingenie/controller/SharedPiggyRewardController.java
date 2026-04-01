package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.SharedPiggyRewardResponse;
import fingenie.com.fingenie.entitlement.EntitlementService;
import fingenie.com.fingenie.service.SharedPiggyRewardService;
import fingenie.com.fingenie.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api-prefix}/piggies/{piggyId}/rewards")
@RequiredArgsConstructor
public class SharedPiggyRewardController {

    private final SharedPiggyRewardService sharedPiggyRewardService;
    private final EntitlementService entitlementService;

    @GetMapping
    public ResponseEntity<List<SharedPiggyRewardResponse>> getUnlocked(
            @PathVariable Long piggyId,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        entitlementService.assertFeature(accountId, "voucher.group.redeem");
        return ResponseEntity.ok(sharedPiggyRewardService.listUnlockedRewards(piggyId, accountId, limit));
    }
}

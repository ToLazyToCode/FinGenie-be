package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.PersonalRewardCatalogItemResponse;
import fingenie.com.fingenie.dto.PersonalRewardOwnedResponse;
import fingenie.com.fingenie.dto.PersonalRewardRedeemResponse;
import fingenie.com.fingenie.entitlement.EntitlementService;
import fingenie.com.fingenie.service.PersonalRewardService;
import fingenie.com.fingenie.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api-prefix}/rewards/personal")
@RequiredArgsConstructor
public class PersonalRewardController {

    private final PersonalRewardService personalRewardService;
    private final EntitlementService entitlementService;

    @GetMapping("/catalog")
    public ResponseEntity<List<PersonalRewardCatalogItemResponse>> getCatalog() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(personalRewardService.getPersonalCatalog(accountId));
    }

    @GetMapping("/owned")
    public ResponseEntity<List<PersonalRewardOwnedResponse>> getOwned(
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(personalRewardService.getOwnedRewards(accountId, limit));
    }

    @PostMapping("/catalog/{rewardId}/redeem")
    public ResponseEntity<PersonalRewardRedeemResponse> redeem(@PathVariable Long rewardId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        entitlementService.assertFeature(accountId, "voucher.personal.redeem");
        return ResponseEntity.ok(personalRewardService.redeem(accountId, rewardId));
    }
}

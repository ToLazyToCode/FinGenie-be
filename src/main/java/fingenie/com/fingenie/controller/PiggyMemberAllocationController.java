package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.utils.SecurityUtils;
import fingenie.com.fingenie.dto.PiggyBankMemberResponse;
import fingenie.com.fingenie.dto.PiggyMemberMonthlyCommitmentRequest;
import fingenie.com.fingenie.dto.PiggyMemberShareWeightRequest;
import fingenie.com.fingenie.entity.SavingContribution;
import fingenie.com.fingenie.service.PiggyBankMemberService;
import fingenie.com.fingenie.service.SavingContributionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("${api-prefix}/piggies")
@RequiredArgsConstructor
public class PiggyMemberAllocationController {

    private final PiggyBankMemberService piggyBankMemberService;
    private final SavingContributionService savingContributionService;

    @PatchMapping("/{piggyId}/members/{memberId}/share-weight")
    public PiggyBankMemberResponse updateShareWeight(
            @PathVariable Long piggyId,
            @PathVariable Long memberId,
            @Valid @RequestBody PiggyMemberShareWeightRequest request
    ) {
        return piggyBankMemberService.updateShareWeight(piggyId, memberId, request.getShareWeight());
    }

    @PatchMapping("/{piggyId}/members/{memberId}/monthly-commitment")
    public PiggyBankMemberResponse updateMonthlyCommitment(
            @PathVariable Long piggyId,
            @PathVariable Long memberId,
            @Valid @RequestBody PiggyMemberMonthlyCommitmentRequest request
    ) {
        return piggyBankMemberService.updateMonthlyCommitment(piggyId, memberId, request.getMonthlyCommitment());
    }

    @PostMapping("/{piggyId}/contribute")
    public ResponseEntity<Map<String, Object>> contribute(
            @PathVariable Long piggyId,
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "MANUAL") SavingContribution.Source source
    ) {
        SavingContribution contribution = savingContributionService.addPiggyContribution(piggyId, amount, source);
        Long accountId = SecurityUtils.getCurrentAccountId();
        BigDecimal memberContributedSum = savingContributionService.getMemberPiggyContributedSum(piggyId, accountId);

        return ResponseEntity.ok(Map.of(
                "contributionId", contribution.getId(),
                "piggyId", piggyId,
                "amount", contribution.getAmount(),
                "source", contribution.getSource(),
                "memberContributedSum", memberContributedSum,
                "createdAt", contribution.getCreatedAt()
        ));
    }
}

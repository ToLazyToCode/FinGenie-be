package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.SavingContributionResponse;
import fingenie.com.fingenie.entity.SavingContribution;
import fingenie.com.fingenie.service.SavingContributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${api-prefix}/savings")
@RequiredArgsConstructor
public class SavingsController {

    private final SavingContributionService savingContributionService;

    @GetMapping("/contributions")
    public List<SavingContributionResponse> getContributions(
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        return savingContributionService.getRecentContributionsForCurrentAccount(limit).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private SavingContributionResponse toResponse(SavingContribution contribution) {
        return SavingContributionResponse.builder()
                .id(contribution.getId())
                .accountId(contribution.getAccountId())
                .targetType(contribution.getTargetType())
                .targetId(contribution.getTargetId())
                .amount(contribution.getAmount())
                .source(contribution.getSource())
                .createdAt(contribution.getCreatedAt())
                .build();
    }
}

package fingenie.com.fingenie.service;

import fingenie.com.fingenie.dto.AutoAllocatePolicyRequest;
import fingenie.com.fingenie.dto.AutoAllocatePolicyResponse;
import fingenie.com.fingenie.entity.AutoAllocatePolicy;
import fingenie.com.fingenie.repository.AutoAllocatePolicyRepository;
import fingenie.com.fingenie.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutoAllocatePolicyService {

    private final AutoAllocatePolicyRepository autoAllocatePolicyRepository;

    @Transactional(readOnly = true)
    public AutoAllocatePolicyResponse getForCurrentAccount() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return autoAllocatePolicyRepository.findByAccountId(accountId)
                .map(this::toResponse)
                .orElseGet(() -> AutoAllocatePolicyResponse.builder()
                        .enabled(false)
                        .updatedAt(null)
                        .build());
    }

    @Transactional
    public AutoAllocatePolicyResponse upsertForCurrentAccount(AutoAllocatePolicyRequest request) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        AutoAllocatePolicy policy = autoAllocatePolicyRepository.findByAccountId(accountId)
                .orElseGet(() -> AutoAllocatePolicy.builder()
                        .accountId(accountId)
                        .enabled(false)
                        .build());

        policy.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        AutoAllocatePolicy saved = autoAllocatePolicyRepository.save(policy);
        return toResponse(saved);
    }

    private AutoAllocatePolicyResponse toResponse(AutoAllocatePolicy policy) {
        return AutoAllocatePolicyResponse.builder()
                .enabled(policy.isEnabled())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}

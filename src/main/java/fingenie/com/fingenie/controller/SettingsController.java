package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.AutoAllocatePolicyRequest;
import fingenie.com.fingenie.dto.AutoAllocatePolicyResponse;
import fingenie.com.fingenie.service.AutoAllocatePolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api-prefix}/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final AutoAllocatePolicyService autoAllocatePolicyService;

    @GetMapping("/auto-allocate-policy")
    public AutoAllocatePolicyResponse getAutoAllocatePolicy() {
        return autoAllocatePolicyService.getForCurrentAccount();
    }

    @PutMapping("/auto-allocate-policy")
    public AutoAllocatePolicyResponse setAutoAllocatePolicy(
            @Valid @RequestBody AutoAllocatePolicyRequest request
    ) {
        return autoAllocatePolicyService.upsertForCurrentAccount(request);
    }
}

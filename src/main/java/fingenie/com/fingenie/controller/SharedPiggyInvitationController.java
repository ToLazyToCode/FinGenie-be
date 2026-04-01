package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.CreateSharedPiggyInvitationRequest;
import fingenie.com.fingenie.dto.SharedPiggyInvitationResponse;
import fingenie.com.fingenie.service.SharedPiggyInvitationService;
import fingenie.com.fingenie.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api-prefix}/shared-piggy-invitations")
@RequiredArgsConstructor
public class SharedPiggyInvitationController {

    private final SharedPiggyInvitationService sharedPiggyInvitationService;

    @PostMapping
    public ResponseEntity<SharedPiggyInvitationResponse> createInvitation(
            @Valid @RequestBody CreateSharedPiggyInvitationRequest request
    ) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(sharedPiggyInvitationService.createInvitation(accountId, request));
    }

    @GetMapping("/incoming")
    public ResponseEntity<List<SharedPiggyInvitationResponse>> getIncomingInvitations() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(sharedPiggyInvitationService.getIncomingInvitations(accountId));
    }

    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<SharedPiggyInvitationResponse> acceptInvitation(@PathVariable Long invitationId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(sharedPiggyInvitationService.acceptInvitation(accountId, invitationId));
    }

    @PostMapping("/{invitationId}/reject")
    public ResponseEntity<SharedPiggyInvitationResponse> rejectInvitation(@PathVariable Long invitationId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(sharedPiggyInvitationService.rejectInvitation(accountId, invitationId));
    }
}

package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.entity.PiggyBankMember;
import fingenie.com.fingenie.dto.PiggyBankMemberRequest;
import fingenie.com.fingenie.dto.PiggyBankMemberResponse;
import fingenie.com.fingenie.service.PiggyBankMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api-prefix}/piggy-bank-members")
@RequiredArgsConstructor
public class PiggyBankMemberController {

    private final PiggyBankMemberService piggyBankMemberService;

    @PostMapping
    public PiggyBankMemberResponse create(@Valid @RequestBody PiggyBankMemberRequest request) {
        return piggyBankMemberService.create(request);
    }

    @GetMapping("/piggy-bank/{piggyId}")
    public List<PiggyBankMemberResponse> getByPiggyBank(@PathVariable Long piggyId) {
        return piggyBankMemberService.getByPiggyBank(piggyId);
    }

    @GetMapping("/me")
    public List<PiggyBankMemberResponse> getByAccount() {
        return piggyBankMemberService.getByAccount();
    }

    @GetMapping("/{id}")
    public PiggyBankMemberResponse getById(@PathVariable Long id) {
        return piggyBankMemberService.getById(id);
    }

    @PutMapping("/{id}/role")
    public PiggyBankMemberResponse updateRole(
            @PathVariable Long id,
            @RequestParam String role
    ) {
        return piggyBankMemberService.updateRole(id, PiggyBankMember.MemberRole.valueOf(role.toUpperCase()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        piggyBankMemberService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Member removed successfully"));
    }
}

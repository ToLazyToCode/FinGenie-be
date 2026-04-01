package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.GamificationProfileDto;
import fingenie.com.fingenie.service.GamificationService;
import fingenie.com.fingenie.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api-prefix}/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/profile")
    public ResponseEntity<GamificationProfileDto> getProfile() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(gamificationService.getProfile(accountId));
    }

    @PostMapping("/add-xp")
    public ResponseEntity<GamificationProfileDto> addXp(@RequestParam("xp") int xp) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(gamificationService.addXp(accountId, xp));
    }
}

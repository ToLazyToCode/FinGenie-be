package fingenie.com.fingenie.admin.controller;

import fingenie.com.fingenie.admin.dto.AdminLoginRequest;
import fingenie.com.fingenie.admin.dto.AdminLoginResponse;
import fingenie.com.fingenie.admin.service.AdminAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api-prefix}/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    /**
     * POST /api/v1/admin/login
     * Public endpoint – permitted in SecurityConfig without authentication.
     */
    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        AdminLoginResponse response = adminAuthService.login(request);
        return ResponseEntity.ok(response);
    }
}

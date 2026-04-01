package fingenie.com.fingenie.admin.service;

import fingenie.com.fingenie.admin.dto.AdminLoginRequest;
import fingenie.com.fingenie.admin.dto.AdminLoginResponse;
import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.Account.Role;
import fingenie.com.fingenie.repository.AccountRepository;
import fingenie.com.fingenie.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AccountRepository accountRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AdminLoginResponse login(AdminLoginRequest request) {
        // Authenticate credentials
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Load the Account entity to check role
        Account account = accountRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new BadCredentialsException("Account not found"));

        // Only ADMIN role is allowed
        if (account.getRole() != Role.ADMIN) {
            log.warn("[AdminAuth] Non-admin login attempt for email={}", request.getEmail());
            throw new BadCredentialsException("Access denied: admin role required");
        }

        // Generate tokens
        String accessToken = jwtService.generateToken(userDetails, account.getId());

        log.info("[AdminAuth] Admin login successful for email={}", request.getEmail());

        return AdminLoginResponse.builder()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .expiresIn(86400L) // 24 hours in seconds
            .adminId(account.getId())
            .email(account.getEmail())
            .name(account.getName())
            .role(account.getRole().name())
            .build();
    }
}

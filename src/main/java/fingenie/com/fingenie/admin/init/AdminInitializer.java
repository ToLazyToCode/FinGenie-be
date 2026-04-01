package fingenie.com.fingenie.admin.init;

import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.Account.AuthProvider;
import fingenie.com.fingenie.entity.Account.Role;
import fingenie.com.fingenie.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures a default admin account exists on startup.
 *
 * Credentials are pulled from application properties:
 *   admin.default.email    (default: admin@fingenie.com)
 *   admin.default.password (default: Admin@1234)
 *
 * The account is only created if it does not already exist.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final AccountRepository accountRepository;
    private final PasswordEncoder    passwordEncoder;

    @Value("${admin.default.email:admin@fingenie.com}")
    private String defaultAdminEmail;

    @Value("${admin.default.password:Admin@1234}")
    private String defaultAdminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (accountRepository.existsByEmail(defaultAdminEmail)) {
            log.info("[AdminInitializer] Admin account already exists: {}", defaultAdminEmail);
            return;
        }

        Account admin = Account.builder()
            .email(defaultAdminEmail)
            .password(passwordEncoder.encode(defaultAdminPassword))
            .name("System Admin")
            .role(Role.ADMIN)
            .provider(AuthProvider.LOCAL)
            .emailVerified(true)
            .build();

        accountRepository.save(admin);
        log.info("[AdminInitializer] Default admin account created: {}", defaultAdminEmail);
    }
}

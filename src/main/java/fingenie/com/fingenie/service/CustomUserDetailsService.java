package fingenie.com.fingenie.service;

import fingenie.com.fingenie.entity.AuthAccount;
import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found: " + email));

        return new AuthAccount(account);
    }
}

package fingenie.com.fingenie.service;

import fingenie.com.fingenie.repository.PiggyBankMemberRepository;
import fingenie.com.fingenie.repository.PiggyBankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PiggyAuthorizationService {

    private final PiggyBankRepository piggyBankRepository;
    private final PiggyBankMemberRepository piggyBankMemberRepository;

    public boolean canViewPiggy(Long piggyId, Long accountId) {
        return hasMembership(piggyId, accountId) || isPrivateOwner(piggyId, accountId);
    }

    public boolean canContributePiggy(Long piggyId, Long accountId) {
        return hasMembership(piggyId, accountId) || isPrivateOwner(piggyId, accountId);
    }

    private boolean hasMembership(Long piggyId, Long accountId) {
        if (piggyId == null || accountId == null) {
            return false;
        }
        return piggyBankMemberRepository.existsByPiggyBankIdAndAccountId(piggyId, accountId);
    }

    private boolean isPrivateOwner(Long piggyId, Long accountId) {
        if (piggyId == null || accountId == null) {
            return false;
        }
        return piggyBankRepository.existsByIdAndWalletAccountIdAndIsSharedFalse(piggyId, accountId);
    }
}

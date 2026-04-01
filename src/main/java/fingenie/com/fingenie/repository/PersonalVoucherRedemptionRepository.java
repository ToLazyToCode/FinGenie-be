package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.PersonalVoucherRedemption;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalVoucherRedemptionRepository extends JpaRepository<PersonalVoucherRedemption, Long> {

    Optional<PersonalVoucherRedemption> findByAccountIdAndRewardCatalogId(Long accountId, Long rewardCatalogId);

    Optional<PersonalVoucherRedemption> findByIdempotencyKey(String idempotencyKey);

    List<PersonalVoucherRedemption> findByAccountIdOrderByClaimedAtDesc(Long accountId);

    List<PersonalVoucherRedemption> findByAccountIdOrderByClaimedAtDesc(Long accountId, Pageable pageable);

    List<PersonalVoucherRedemption> findByAccountIdAndRewardCatalogIdIn(
            Long accountId,
            Collection<Long> rewardCatalogIds
    );
}

package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.SharedPiggyInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SharedPiggyInvitationRepository extends JpaRepository<SharedPiggyInvitation, Long> {

    List<SharedPiggyInvitation> findByInviteeIdAndStatusOrderByCreatedAtDesc(
            Long inviteeId,
            SharedPiggyInvitation.Status status
    );

    Optional<SharedPiggyInvitation> findByIdAndInviteeId(Long id, Long inviteeId);

    boolean existsByWalletIdAndStatus(Long walletId, SharedPiggyInvitation.Status status);

    boolean existsByWalletIdAndInviteeIdAndStatus(
            Long walletId,
            Long inviteeId,
            SharedPiggyInvitation.Status status
    );

    @Modifying
    @Query("""
            update SharedPiggyInvitation invitation
            set invitation.status = :expiredStatus,
                invitation.respondedAt = :now
            where invitation.status = :pendingStatus
              and invitation.expiresAt < :now
            """)
    int expirePendingInvitations(
            @Param("pendingStatus") SharedPiggyInvitation.Status pendingStatus,
            @Param("expiredStatus") SharedPiggyInvitation.Status expiredStatus,
            @Param("now") LocalDateTime now
    );
}

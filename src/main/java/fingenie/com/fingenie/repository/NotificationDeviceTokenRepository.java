package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.NotificationDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationDeviceTokenRepository extends JpaRepository<NotificationDeviceToken, Long> {

    Optional<NotificationDeviceToken> findByAccountIdAndDeviceToken(Long accountId, String deviceToken);

    List<NotificationDeviceToken> findByAccountIdAndEnabledTrue(Long accountId);
}

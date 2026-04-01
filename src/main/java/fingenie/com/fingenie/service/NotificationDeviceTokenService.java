package fingenie.com.fingenie.service;

import fingenie.com.fingenie.dto.NotificationDeviceTokenRequest;
import fingenie.com.fingenie.dto.NotificationDeviceTokenResponse;
import fingenie.com.fingenie.entity.NotificationDeviceToken;
import fingenie.com.fingenie.repository.NotificationDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NotificationDeviceTokenService {

    private final NotificationDeviceTokenRepository notificationDeviceTokenRepository;

    @Transactional
    public NotificationDeviceTokenResponse registerOrUpdate(Long accountId, NotificationDeviceTokenRequest request) {
        String normalizedToken = request.getDeviceToken().trim();
        Timestamp now = Timestamp.from(Instant.now());

        NotificationDeviceToken token = notificationDeviceTokenRepository
                .findByAccountIdAndDeviceToken(accountId, normalizedToken)
                .orElseGet(() -> NotificationDeviceToken.builder()
                        .accountId(accountId)
                        .deviceToken(normalizedToken)
                        .lastSeenAt(now)
                        .enabled(true)
                        .build());

        token.setPlatform(request.getPlatform());
        token.setEnabled(request.getEnabled() == null || request.getEnabled());
        token.setLastSeenAt(now);

        NotificationDeviceToken saved = notificationDeviceTokenRepository.save(token);
        return toResponse(saved);
    }

    @Transactional
    public void disableToken(Long accountId, String deviceToken) {
        notificationDeviceTokenRepository.findByAccountIdAndDeviceToken(accountId, deviceToken)
                .ifPresent(token -> {
                    token.setEnabled(false);
                    token.setLastSeenAt(Timestamp.from(Instant.now()));
                    notificationDeviceTokenRepository.save(token);
                });
    }

    private NotificationDeviceTokenResponse toResponse(NotificationDeviceToken entity) {
        return NotificationDeviceTokenResponse.builder()
                .id(entity.getId())
                .accountId(entity.getAccountId())
                .deviceToken(entity.getDeviceToken())
                .platform(entity.getPlatform())
                .enabled(entity.getEnabled())
                .lastSeenAt(entity.getLastSeenAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

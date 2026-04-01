package fingenie.com.fingenie.service;

import fingenie.com.fingenie.common.error.exceptions.SystemExceptions;
import fingenie.com.fingenie.config.CreationGuardrailConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
@RequiredArgsConstructor
public class CreationThrottleService {

    private final CreationGuardrailConfig guardrailConfig;
    private final Map<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

    public void assertWalletCreateAllowed(Long accountId) {
        CreationGuardrailConfig.CreationThrottle throttle = guardrailConfig.getCreationThrottle();
        if (!throttle.isEnabled()) {
            return;
        }
        assertAllowed(
                "wallet-create",
                accountId,
                throttle.getWalletCreateMax(),
                throttle.getWindowSeconds()
        );
    }

    public void assertPiggyCreateAllowed(Long accountId) {
        CreationGuardrailConfig.CreationThrottle throttle = guardrailConfig.getCreationThrottle();
        if (!throttle.isEnabled()) {
            return;
        }
        assertAllowed(
                "piggy-create",
                accountId,
                throttle.getPiggyCreateMax(),
                throttle.getWindowSeconds()
        );
    }

    private void assertAllowed(String operation, Long accountId, int limit, int windowSeconds) {
        if (accountId == null || limit <= 0 || windowSeconds <= 0) {
            return;
        }

        long nowEpochMs = Instant.now().toEpochMilli();
        long windowStart = nowEpochMs - (windowSeconds * 1000L);
        String bucketKey = operation + ":" + accountId;
        Deque<Long> timestamps = buckets.computeIfAbsent(bucketKey, key -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= limit) {
                throw new SystemExceptions.RateLimitExceededException(operation, limit, windowSeconds);
            }

            timestamps.addLast(nowEpochMs);
        }
    }
}

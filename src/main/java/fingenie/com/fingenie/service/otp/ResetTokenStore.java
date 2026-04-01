package fingenie.com.fingenie.service.otp;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-based reset token store for forgot password.
 * Key: reset:token:{token} -> email
 * TTL: 10 minutes
 */
@Component
@RequiredArgsConstructor
public class ResetTokenStore {

    private static final String PREFIX = "reset:token:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, Object> redisTemplate;

    public String createToken(String email) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(PREFIX + token, email, TTL);
        return token;
    }

    public Optional<String> getEmailAndDelete(String token) {
        String key = PREFIX + token;
        Object email = redisTemplate.opsForValue().get(key);
        if (email != null) {
            redisTemplate.delete(key);
            return Optional.of((String) email);
        }
        return Optional.empty();
    }
}

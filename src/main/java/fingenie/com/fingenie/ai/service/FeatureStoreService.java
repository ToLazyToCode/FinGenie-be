package fingenie.com.fingenie.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fingenie.com.fingenie.ai.entity.FeatureSnapshot;
import fingenie.com.fingenie.ai.repository.FeatureSnapshotRepository;
import fingenie.com.fingenie.entity.Category;
import fingenie.com.fingenie.entity.Transaction;
import fingenie.com.fingenie.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * FeatureStoreService - Manages online (Redis) and offline (Postgres) feature stores.
 * 
 * Online Features (Redis - Real-time):
 * - Rolling 7-day spend total and by category
 * - Category distribution percentages
 * - Volatility score
 * - Tracking frequency
 * - Emotional spending score
 * 
 * Offline Features (Postgres - Historical):
 * - Daily feature snapshots for model training
 * - Feature versioning for reproducibility
 * - Historical data for drift detection
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureStoreService {

    private final StringRedisTemplate redisTemplate;
    private final FeatureSnapshotRepository featureSnapshotRepository;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    private static final String FEATURE_PREFIX = "user:features:";
    private static final Duration FEATURE_TTL = Duration.ofHours(24);
    private static final String FEATURE_VERSION = "v1";

    // ============================================
    // Online Feature Store (Redis)
    // ============================================

    /**
     * Get all features for a user from Redis (online store).
     * Falls back to computing from DB if not cached.
     */
    public Map<String, Object> getOnlineFeatures(Long userId) {
        String key = FEATURE_PREFIX + userId;
        
        // Try to get from Redis
        Map<Object, Object> cached = redisTemplate.opsForHash().entries(key);
        
        if (!cached.isEmpty()) {
            log.debug("Retrieved features from Redis for userId={}", userId);
            Map<String, Object> features = new HashMap<>();
            cached.forEach((k, v) -> features.put(k.toString(), deserializeFromRedis(v)));
            return features;
        }
        
        // Compute and cache if not found
        log.debug("Computing features for userId={}", userId);
        return computeAndCacheFeatures(userId);
    }

    /**
     * Compute current features and cache in Redis.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> computeAndCacheFeatures(Long userId) {
        Map<String, Object> features = computeFeatures(userId);
        cacheFeatures(userId, features);
        return features;
    }

    /**
     * Compute all features for a user from transaction history.
     */
    public Map<String, Object> computeFeatures(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        
        // Get transactions for the past 30 days (for trend analysis)
        List<Transaction> transactions30d = transactionRepository
                .findByAccountIdAndDateRange(
                        userId,
                        java.sql.Date.valueOf(thirtyDaysAgo.toLocalDate()),
                        java.sql.Date.valueOf(now.toLocalDate()));
        
        // Filter for 7-day window
        List<Transaction> transactions7d = transactions30d.stream()
                .filter(t -> t.getTransactionDate().toLocalDate().isAfter(sevenDaysAgo.toLocalDate()))
                .collect(Collectors.toList());

        Map<String, Object> features = new HashMap<>();
        
        // Rolling 7-day spend
        BigDecimal rolling7dSpend = transactions7d.stream()
                .map(Transaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        features.put("rolling_7d_spend", rolling7dSpend);

        // Spend by category (7d)
        Map<String, BigDecimal> spendByCategory = transactions7d.stream()
                .filter(t -> t.getCategory() != null)
                .filter(t -> t.getAmount() != null)
                .collect(Collectors.groupingBy(
                        this::resolveCategoryKey,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));
        features.put("rolling_7d_spend_by_category", spendByCategory);

        // Category distribution (percentages)
        Map<String, BigDecimal> categoryDistribution = new HashMap<>();
        if (rolling7dSpend.compareTo(BigDecimal.ZERO) > 0) {
            spendByCategory.forEach((cat, amount) -> 
                    categoryDistribution.put(cat, 
                            amount.divide(rolling7dSpend, 4, RoundingMode.HALF_UP)));
        }
        features.put("category_distribution", categoryDistribution);

        // Volatility score (coefficient of variation of daily spending)
        BigDecimal volatilityScore = calculateVolatilityScore(transactions30d);
        features.put("volatility_score", volatilityScore);

        // Tracking frequency (transactions per day)
        BigDecimal trackingFrequency = transactions7d.isEmpty() ? 
                BigDecimal.ZERO : 
                new BigDecimal(transactions7d.size()).divide(new BigDecimal(7), 2, RoundingMode.HALF_UP);
        features.put("tracking_frequency", trackingFrequency);

        // Average transaction amount
        BigDecimal avgTransactionAmount = transactions7d.isEmpty() ? 
                BigDecimal.ZERO :
                rolling7dSpend.divide(new BigDecimal(transactions7d.size()), 2, RoundingMode.HALF_UP);
        features.put("avg_transaction_amount", avgTransactionAmount);

        // Transaction count
        features.put("transaction_count_7d", transactions7d.size());

        // Weekday vs weekend ratio
        BigDecimal weekdayVsWeekendRatio = calculateWeekdayWeekendRatio(transactions7d);
        features.put("weekday_vs_weekend_ratio", weekdayVsWeekendRatio);

        // Time of day pattern
        Map<String, BigDecimal> timeOfDayPattern = calculateTimeOfDayPattern(transactions7d);
        features.put("time_of_day_pattern", timeOfDayPattern);

        // Feature version
        features.put("feature_version", FEATURE_VERSION);
        features.put("computed_at", LocalDateTime.now().toString());

        // Compute hash for reproducibility
        String featureHash = computeFeatureHash(features);
        features.put("feature_hash", featureHash);

        return features;
    }

    /**
     * Cache features in Redis with TTL.
     */
    private void cacheFeatures(Long userId, Map<String, Object> features) {
        String key = FEATURE_PREFIX + userId;
        
        // Serialize all values as JSON strings for Python compatibility
        Map<String, String> flatFeatures = new HashMap<>();
        features.forEach((k, v) -> {
            flatFeatures.put(k, serializeForRedis(v));
        });
        
        redisTemplate.opsForHash().putAll(key, flatFeatures);
        redisTemplate.expire(key, FEATURE_TTL.toSeconds(), TimeUnit.SECONDS);
        
        log.debug("Cached features for userId={} with TTL={}", userId, FEATURE_TTL);
    }

    /**
     * Invalidate cached features for a user.
     */
    public void invalidateFeatures(Long userId) {
        String key = FEATURE_PREFIX + userId;
        redisTemplate.delete(key);
        log.debug("Invalidated features cache for userId={}", userId);
    }

    /**
     * Update features incrementally after a transaction.
     */
    public void updateFeaturesAfterTransaction(Long userId, Transaction transaction) {
        updateFeatures(userId);
    }

    /**
     * Update features incrementally after a transaction event.
     */
    public void updateFeaturesAfterTransaction(Long userId) {
        updateFeatures(userId);
    }

    /**
     * Recompute and refresh all cached online features for a user.
     */
    @Transactional(readOnly = true)
    public void updateFeatures(Long userId) {
        // For MVP, simple invalidate and recompute
        // Future: Implement incremental updates for efficiency
        invalidateFeatures(userId);
        computeAndCacheFeatures(userId);
        log.debug("Updated features for userId={}", userId);
    }

    // ============================================
    // Offline Feature Store (Postgres)
    // ============================================

    /**
     * Save a feature snapshot to Postgres for offline storage.
     */
    @Transactional
    public FeatureSnapshot saveFeatureSnapshot(Long userId) {
        Map<String, Object> features = getOnlineFeatures(userId);
        
        FeatureSnapshot snapshot = FeatureSnapshot.builder()
                .userId(userId)
                .snapshotDate(LocalDate.now())
                .featureHash((String) features.get("feature_hash"))
                .rolling7dSpend(asBigDecimal(features.get("rolling_7d_spend"), BigDecimal.ZERO))
                .rolling7dSpendByCategory(asBigDecimalMap(features.get("rolling_7d_spend_by_category")))
                .categoryDistribution(asBigDecimalMap(features.get("category_distribution")))
                .volatilityScore(asBigDecimal(features.get("volatility_score"), BigDecimal.ZERO))
                .trackingFrequency(asBigDecimal(features.get("tracking_frequency"), BigDecimal.ZERO))
                .avgTransactionAmount(asBigDecimal(features.get("avg_transaction_amount"), BigDecimal.ZERO))
                .transactionCount7d(asInteger(features.get("transaction_count_7d"), 0))
                .weekdayVsWeekendRatio(asBigDecimal(features.get("weekday_vs_weekend_ratio"), BigDecimal.ONE))
                .timeOfDayPattern(asBigDecimalMap(features.get("time_of_day_pattern")))
                .featureJson(features)
                .featureVersion(FEATURE_VERSION)
                .build();
        
        return featureSnapshotRepository.save(snapshot);
    }

    /**
     * Daily batch job to create feature snapshots for all active users.
     */
    @Scheduled(cron = "0 0 1 * * ?") // Run at 1 AM daily
    @Transactional
    public void dailyFeatureSnapshotBatch() {
        log.info("Starting daily feature snapshot batch job");
        
        // Get users with recent transactions
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<Long> activeUserIds = transactionRepository
                .findDistinctAccountIdsWithTransactionsAfter(java.sql.Date.valueOf(yesterday.minusDays(30)));
        
        int successCount = 0;
        int errorCount = 0;
        
        for (Long userId : activeUserIds) {
            try {
                saveFeatureSnapshot(userId);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to create feature snapshot for userId={}: {}", userId, e.getMessage());
                errorCount++;
            }
        }
        
        log.info("Completed daily feature snapshot batch: success={}, errors={}", successCount, errorCount);
    }

    // ============================================
    // Helper Methods
    // ============================================

    private BigDecimal calculateVolatilityScore(List<Transaction> transactions) {
        if (transactions.size() < 2) {
            return new BigDecimal("0.50"); // Default moderate volatility
        }

        // Group by date and calculate daily totals
        Map<LocalDate, BigDecimal> dailyTotals = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTransactionDate().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        if (dailyTotals.size() < 2) {
            return new BigDecimal("0.50");
        }

        // Calculate mean
        BigDecimal sum = dailyTotals.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mean = sum.divide(new BigDecimal(dailyTotals.size()), 4, RoundingMode.HALF_UP);

        if (mean.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Calculate standard deviation
        BigDecimal sumSquaredDiff = dailyTotals.values().stream()
                .map(v -> v.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal variance = sumSquaredDiff.divide(new BigDecimal(dailyTotals.size()), 4, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        // Coefficient of variation (normalized to 0-1 scale)
        BigDecimal cv = stdDev.divide(mean, 4, RoundingMode.HALF_UP);
        return cv.min(BigDecimal.ONE); // Cap at 1.0
    }

    private BigDecimal calculateWeekdayWeekendRatio(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return BigDecimal.ONE;
        }

        BigDecimal weekdaySpend = BigDecimal.ZERO;
        BigDecimal weekendSpend = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            int dayOfWeek = t.getTransactionDate().toLocalDate().getDayOfWeek().getValue();
            if (dayOfWeek >= 6) { // Saturday or Sunday
                weekendSpend = weekendSpend.add(t.getAmount());
            } else {
                weekdaySpend = weekdaySpend.add(t.getAmount());
            }
        }

        if (weekendSpend.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("2.0"); // All weekday spending
        }

        return weekdaySpend.divide(weekendSpend, 4, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> calculateTimeOfDayPattern(List<Transaction> transactions) {
        Map<String, BigDecimal> pattern = new HashMap<>();
        pattern.put("morning", BigDecimal.ZERO);   // 6-12
        pattern.put("afternoon", BigDecimal.ZERO); // 12-18
        pattern.put("evening", BigDecimal.ZERO);   // 18-22
        pattern.put("night", BigDecimal.ZERO);     // 22-6

        // Since transactions don't have time, distribute evenly
        // In real implementation, would use transaction timestamp
        BigDecimal total = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            pattern.put("morning", new BigDecimal("0.30"));
            pattern.put("afternoon", new BigDecimal("0.35"));
            pattern.put("evening", new BigDecimal("0.25"));
            pattern.put("night", new BigDecimal("0.10"));
        }

        return pattern;
    }

    private String resolveCategoryKey(Transaction transaction) {
        Category category = transaction.getCategory();
        if (category == null) {
            return "UNCATEGORIZED";
        }
        String categoryName = category.getCategoryName();
        if (categoryName == null || categoryName.isBlank()) {
            return "CATEGORY_" + category.getId();
        }
        return categoryName;
    }

    private String computeFeatureHash(Map<String, Object> features) {
        try {
            // Sort keys for consistent hashing
            TreeMap<String, Object> sorted = new TreeMap<>(features);
            sorted.remove("computed_at"); // Exclude timestamp
            sorted.remove("feature_hash"); // Exclude hash itself
            
            String json = sorted.toString();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to compute feature hash", e);
            return UUID.randomUUID().toString();
        }
    }

    private String serializeForRedis(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to JSON serialize feature value, falling back to quoted string", e);
            return "\"" + String.valueOf(value).replace("\"", "\\\"") + "\"";
        }
    }

    private Object deserializeFromRedis(Object value) {
        if (!(value instanceof String str)) {
            return value;
        }

        try {
            return objectMapper.readValue(str, Object.class);
        } catch (Exception e) {
            log.warn("Failed to JSON deserialize feature value, using raw string", e);
            return str;
        }
    }

    private BigDecimal asBigDecimal(Object value, BigDecimal defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value instanceof String str) {
            try {
                return new BigDecimal(str);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private Integer asInteger(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private Map<String, BigDecimal> asBigDecimalMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Collections.emptyMap();
        }

        Map<String, BigDecimal> converted = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            BigDecimal parsed = asBigDecimal(entry.getValue(), null);
            if (parsed != null) {
                converted.put(entry.getKey().toString(), parsed);
            }
        }
        return converted;
    }
}

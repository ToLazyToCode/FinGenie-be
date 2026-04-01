package fingenie.com.fingenie.service;

import fingenie.com.fingenie.dto.AIPredictionDto;
import fingenie.com.fingenie.entity.AIPrediction;
import fingenie.com.fingenie.repository.AIPredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIPredictionService {

    private final AIPredictionRepository repository;

    // Simple rule-based monthly spending prediction stub
    public AIPredictionDto predictMonthly(Long accountId) {
        Map<String, Object> result = new HashMap<>();
        result.put("monthly_spend_estimate", 1234.56);
        result.put("overspending_risk", "LOW");
        result.put("generatedAt", Instant.now().toString());

        String json = result.toString();

        AIPrediction saved = repository.save(
                AIPrediction.builder()
                        .accountId(accountId)
                        .predictionJson(json)
                        .build()
        );

        return AIPredictionDto.builder()
                .id(saved.getId())
                .accountId(saved.getAccountId())
                .predictionJson(saved.getPredictionJson())
                .createdAt(toInstant(saved.getCreatedAt()))
                .build();
    }

    @Cacheable(value = "ai.prediction.latest", key = "#accountId")
    public AIPredictionDto getLatest(Long accountId) {
        return repository.findTopByAccountIdOrderByCreatedAtDesc(accountId)
                .map(p -> AIPredictionDto.builder()
                        .id(p.getId())
                        .accountId(p.getAccountId())
                        .predictionJson(p.getPredictionJson())
                        .createdAt(toInstant(p.getCreatedAt()))
                        .build())
                .orElse(null);
    }

    private Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}

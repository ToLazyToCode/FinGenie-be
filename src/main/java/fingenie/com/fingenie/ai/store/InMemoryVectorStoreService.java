package fingenie.com.fingenie.ai.store;

import fingenie.com.fingenie.ai.service.VectorStoreService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ai.vector-store", havingValue = "INMEMORY", matchIfMissing = true)
public class InMemoryVectorStoreService implements VectorStoreService {

    private final Map<Long, List<float[]>> store = new HashMap<>();

    @Override
    public void storeEmbedding(Long userId, float[] vector, String sourceType, Long referenceId) {
        store.computeIfAbsent(userId, k -> new ArrayList<>()).add(vector);
    }

    @Override
    public float[][] searchSimilar(Long userId, float[] query, int k) {
        List<float[]> list = store.getOrDefault(userId, List.of());
        int n = Math.min(k, list.size());
        float[][] out = new float[n][];
        for (int i = 0; i < n; i++) out[i] = list.get(i);
        return out;
    }
}

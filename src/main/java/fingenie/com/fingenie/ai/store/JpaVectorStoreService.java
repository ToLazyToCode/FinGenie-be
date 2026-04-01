package fingenie.com.fingenie.ai.store;

import fingenie.com.fingenie.ai.entity.AIEmbedding;
import fingenie.com.fingenie.ai.repository.AIEmbeddingRepository;
import fingenie.com.fingenie.ai.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.vector-store", havingValue = "JPA")
public class JpaVectorStoreService implements VectorStoreService {

    private final AIEmbeddingRepository repo;

    @Override
    public void storeEmbedding(Long userId, float[] vector, String sourceType, Long referenceId) {
        byte[] bytes = floatArrayToBytes(vector);
        AIEmbedding e = AIEmbedding.builder().userId(userId).vector(bytes).sourceType(sourceType).referenceId(referenceId).build();
        repo.save(e);
    }

    @Override
    public float[][] searchSimilar(Long userId, float[] query, int k) {
        List<AIEmbedding> list = repo.findByUserId(userId);
        int n = Math.min(k, list.size());
        float[][] out = new float[n][];
        for (int i = 0; i < n; i++) {
            out[i] = bytesToFloatArray(list.get(i).getVector());
        }
        return out;
    }

    private static byte[] floatArrayToBytes(float[] arr) {
        ByteBuffer bb = ByteBuffer.allocate(arr.length * 4);
        for (float f : arr) bb.putFloat(f);
        return bb.array();
    }

    private static float[] bytesToFloatArray(byte[] bytes) {
        int n = bytes.length / 4;
        float[] arr = new float[n];
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        for (int i = 0; i < n; i++) arr[i] = bb.getFloat();
        return arr;
    }
}

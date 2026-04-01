package fingenie.com.fingenie.ai.service;

public interface VectorStoreService {
    void storeEmbedding(Long userId, float[] vector, String sourceType, Long referenceId);

    float[][] searchSimilar(Long userId, float[] query, int k);
}

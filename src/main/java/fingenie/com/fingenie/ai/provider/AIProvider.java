package fingenie.com.fingenie.ai.provider;

import java.util.Map;

public interface AIProvider {

    String generateText(String prompt, Map<String, Object> options);

    float[] generateEmbedding(String text);

    boolean healthCheck();
}

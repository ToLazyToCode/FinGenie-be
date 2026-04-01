package fingenie.com.fingenie.ai.core;

import fingenie.com.fingenie.ai.provider.AIProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component("stubProvider")
@Profile("dev")
public class AIRuntimeStub implements AIProvider {

    @Override
    public String generateText(String prompt, Map<String, Object> options) {
        return "Echo: " + (prompt == null ? "" : prompt);
    }

    @Override
    public float[] generateEmbedding(String text) {
        if (text == null) text = "";
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        float[] vec = new float[Math.min(64, Math.max(8, bytes.length))];
        int len = vec.length;
        for (int i = 0; i < len; i++) {
            vec[i] = (bytes[i % bytes.length] & 0xff) / 255.0f;
        }
        return vec;
    }

    @Override
    public boolean healthCheck() {
        return true;
    }
}

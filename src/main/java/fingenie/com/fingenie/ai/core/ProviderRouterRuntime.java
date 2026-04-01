package fingenie.com.fingenie.ai.core;

import fingenie.com.fingenie.ai.provider.AIProvider;
import fingenie.com.fingenie.ai.router.AIProviderRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@Primary
@RequiredArgsConstructor
public class ProviderRouterRuntime implements AIRuntime {

    private static final String DEFAULT_MODE = "AUTO";

    private final AIProviderRouter providerRouter;

    @Override
    public AIResponse generate(AIRequest request) {
        String prompt = request != null && request.getPrompt() != null ? request.getPrompt() : "";
        String mode = extractMode(request);
        AIProvider provider = providerRouter.selectProvider(mode, prompt);

        Map<String, Object> options = request != null && request.getContext() != null
                ? request.getContext()
                : Map.of();

        String text = provider.generateText(prompt, options);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", provider.getClass().getSimpleName());
        metadata.put("mode", mode);
        return new AIResponse(text, 0.85d, metadata);
    }

    @Override
    public double[] embed(String text) {
        String safeText = text == null ? "" : text;
        AIProvider provider = providerRouter.selectProvider(DEFAULT_MODE, safeText);
        float[] embedding = provider.generateEmbedding(safeText);
        if (embedding == null || embedding.length == 0) {
            return deterministicFallbackVector(safeText);
        }

        double[] out = new double[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            out[i] = embedding[i];
        }
        return out;
    }

    private String extractMode(AIRequest request) {
        if (request != null && request.getContext() != null) {
            Object mode = request.getContext().get("mode");
            if (mode instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        return DEFAULT_MODE;
    }

    private double[] deterministicFallbackVector(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        int size = Math.min(64, Math.max(8, bytes.length == 0 ? 8 : bytes.length));
        double[] vector = new double[size];
        for (int i = 0; i < size; i++) {
            vector[i] = (bytes.length == 0 ? 0 : (bytes[i % bytes.length] & 0xff)) / 255.0d;
        }
        return vector;
    }
}


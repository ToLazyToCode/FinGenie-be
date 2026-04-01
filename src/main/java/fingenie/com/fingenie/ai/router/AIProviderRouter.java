package fingenie.com.fingenie.ai.router;

import fingenie.com.fingenie.ai.provider.AIProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AIProviderRouter {

    private final Map<String, AIProvider> providers; // injected by Spring by bean name

    public AIProvider selectProvider(String mode, String prompt) {
        AIProvider gemini = providers.get("geminiProvider");
        if (isHealthy(gemini)) {
            return gemini;
        }

        AIProvider ollama = providers.get("ollamaProvider");
        if (isHealthy(ollama)) {
            return ollama;
        }

        // Dev-only fallback provider
        AIProvider stub = providers.get("stubProvider");
        if (stub != null) {
            return stub;
        }

        // Last-resort fallback for non-dev: choose first configured provider even if unhealthy
        if (gemini != null) {
            return gemini;
        }
        if (ollama != null) {
            return ollama;
        }

        if (!providers.isEmpty()) {
            return providers.values().iterator().next();
        }

        throw new IllegalStateException("No AI providers available");
    }

    private boolean isHealthy(AIProvider provider) {
        if (provider == null) {
            return false;
        }
        try {
            return provider.healthCheck();
        } catch (Exception ex) {
            return false;
        }
    }
}

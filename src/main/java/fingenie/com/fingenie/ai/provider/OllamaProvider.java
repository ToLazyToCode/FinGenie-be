package fingenie.com.fingenie.ai.provider;

import fingenie.com.fingenie.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component("ollamaProvider")
@RequiredArgsConstructor
public class OllamaProvider implements AIProvider {

    private final RestTemplate restTemplate;
    private final AiProperties aiProperties;

    @Override
    public String generateText(String prompt, Map<String, Object> options) {
        try {
            String url = aiProperties.getOllama().getUrl() + "/api/generate";
            Map<String, Object> body = new HashMap<>();
            body.put("model", aiProperties.getOllama().getModel());
            body.put("prompt", prompt);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
            Map<String, Object> resp = restTemplate.postForObject(url, req, Map.class);
            if (resp != null && resp.containsKey("result")) {
                return resp.get("result").toString();
            }
            return "[Ollama no result]";
        } catch (Exception ex) {
            // fallback deterministic reply
            return "OLLAMA_FALLBACK: " + (prompt == null ? "" : prompt.substring(0, Math.min(200, prompt.length())));
        }
    }

    @Override
    public float[] generateEmbedding(String text) {
        // Ollama may not provide embeddings in this stub. Return deterministic small vector.
        return new float[]{(float) text.length(), 0.0f, 1.0f};
    }

    @Override
    public boolean healthCheck() {
        try {
            String url = aiProperties.getOllama().getUrl() + "/api/status";
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            return resp != null;
        } catch (Exception ex) {
            return false;
        }
    }
}

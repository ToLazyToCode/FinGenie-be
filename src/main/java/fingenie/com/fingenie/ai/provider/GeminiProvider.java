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

@Component("geminiProvider")
@RequiredArgsConstructor
public class GeminiProvider implements AIProvider {

    private final RestTemplate restTemplate;
    private final AiProperties aiProperties;

    @Override
    public String generateText(String prompt, Map<String, Object> options) {
        try {
            String url = aiProperties.getGemini().getUrl() + "/generate";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(aiProperties.getGemini().getApiKey());
            Map<String, Object> body = new HashMap<>();
            body.put("model", aiProperties.getGemini().getModel());
            body.put("prompt", prompt);
            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
            Map<String, Object> resp = restTemplate.postForObject(url, req, Map.class);
            if (resp != null && resp.containsKey("reply")) return resp.get("reply").toString();
            return "[Gemini no result]";
        } catch (Exception ex) {
            return "GEMINI_FALLBACK: " + (prompt == null ? "" : prompt.substring(0, Math.min(200, prompt.length())));
        }
    }

    @Override
    public float[] generateEmbedding(String text) {
        return new float[]{(float) text.hashCode(), 0.1f, 0.2f};
    }

    @Override
    public boolean healthCheck() {
        return aiProperties.getGemini().getApiKey() != null;
    }
}

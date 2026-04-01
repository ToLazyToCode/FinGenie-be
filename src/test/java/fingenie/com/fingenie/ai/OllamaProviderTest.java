package fingenie.com.fingenie.ai;

import fingenie.com.fingenie.ai.provider.OllamaProvider;
import fingenie.com.fingenie.config.AiProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class OllamaProviderTest {

    @Test
    void generateTextFallbackAndHealth() {
        RestTemplate rt = Mockito.mock(RestTemplate.class);
        AiProperties props = new AiProperties();
        props.getOllama().setUrl("http://localhost:11434");
        OllamaProvider p = new OllamaProvider(rt, props);

        // Simulate RestTemplate throwing exception -> fallback
        when(rt.postForObject(anyString(), any(), eq(Map.class))).thenThrow(new RuntimeException("no host"));
        String reply = p.generateText("hello world", Map.of());
        assertTrue(reply.startsWith("OLLAMA_FALLBACK") || reply.startsWith("[Ollama") || reply.contains("fallback"));

        // health check getForObject -> throw -> false
        when(rt.getForObject(anyString(), eq(String.class))).thenThrow(new RuntimeException("no host"));
        assertFalse(p.healthCheck());
    }
}

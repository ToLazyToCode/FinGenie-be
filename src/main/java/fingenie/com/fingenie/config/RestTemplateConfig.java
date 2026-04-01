package fingenie.com.fingenie.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate configuration for external service calls.
 */
@Configuration
public class RestTemplateConfig {

    @Value("${ai.service.timeout-ms:300}")
    private int aiServiceTimeoutMs;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(aiServiceTimeoutMs))
                .setReadTimeout(Duration.ofMillis(aiServiceTimeoutMs * 2)) // Read timeout slightly longer
                .build();
    }
}

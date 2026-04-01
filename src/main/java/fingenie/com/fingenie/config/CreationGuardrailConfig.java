package fingenie.com.fingenie.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "fingenie.guardrails")
@Getter
@Setter
public class CreationGuardrailConfig {

    private Wallet wallet = new Wallet();
    private Piggy piggy = new Piggy();
    private CreationThrottle creationThrottle = new CreationThrottle();

    @Getter
    @Setter
    public static class Wallet {
        private int maxPerAccount = 10;
    }

    @Getter
    @Setter
    public static class Piggy {
        private int maxPerAccount = 15;
    }

    @Getter
    @Setter
    public static class CreationThrottle {
        private boolean enabled = true;
        private int windowSeconds = 60;
        private int walletCreateMax = 5;
        private int piggyCreateMax = 5;
    }
}

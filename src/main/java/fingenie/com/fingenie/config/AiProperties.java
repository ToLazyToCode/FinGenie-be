package fingenie.com.fingenie.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private Ollama ollama = new Ollama();
    private Gemini gemini = new Gemini();
    private String defaultMode = "AUTO";

    public static class Ollama {
        private String url = "http://localhost:11434";
        private String model = "llama3";
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }

    public static class Gemini {
        private String apiKey;
        private String model = "gemini-pro";
        private String url = "https://api.gemini.example";
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public Ollama getOllama() { return ollama; }
    public void setOllama(Ollama ollama) { this.ollama = ollama; }
    public Gemini getGemini() { return gemini; }
    public void setGemini(Gemini gemini) { this.gemini = gemini; }
    public String getDefaultMode() { return defaultMode; }
    public void setDefaultMode(String defaultMode) { this.defaultMode = defaultMode; }
}

package fingenie.com.fingenie.ai.core;

import java.util.Map;

public class AIResponse {
    private String text;
    private double confidence;
    private Map<String, Object> metadata;

    public AIResponse() {}

    public AIResponse(String text, double confidence, Map<String, Object> metadata) {
        this.text = text;
        this.confidence = confidence;
        this.metadata = metadata;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}

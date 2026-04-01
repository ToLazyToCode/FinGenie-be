package fingenie.com.fingenie.ai.core;

import java.util.Map;

public class AIRequest {
    private String prompt;
    private Map<String, Object> context;
    private Integer maxTokens;

    public AIRequest() {}

    public AIRequest(String prompt, Map<String, Object> context, Integer maxTokens) {
        this.prompt = prompt;
        this.context = context;
        this.maxTokens = maxTokens;
    }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
}

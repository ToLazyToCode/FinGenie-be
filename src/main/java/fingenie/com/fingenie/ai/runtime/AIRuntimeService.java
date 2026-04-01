package fingenie.com.fingenie.ai.runtime;

import fingenie.com.fingenie.ai.builder.ContextBuilder;
import fingenie.com.fingenie.ai.builder.PromptBuilder;
import fingenie.com.fingenie.ai.core.AIRequest;
import fingenie.com.fingenie.ai.core.AIResponse;
import fingenie.com.fingenie.ai.core.AIRuntime;
import fingenie.com.fingenie.ai.entity.AIConversationMessage;
import fingenie.com.fingenie.ai.repository.AIConversationRepository;
import fingenie.com.fingenie.ai.router.AIProviderRouter;
import fingenie.com.fingenie.ai.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIRuntimeService {

    private final AIRuntime aiRuntime;
    private final AIProviderRouter providerRouter;
    private final PromptBuilder promptBuilder;
    private final ContextBuilder contextBuilder;
    private final VectorStoreService vectorStoreService;
    private final AIConversationRepository conversationRepository;

    /**
     * Generate AI response with full context and memory
     */
    @Transactional
    public AIResponse generateWithMemory(Long userId, String category, String userMessage, List<String> extras) {
        try {
            // 1. Build context
            String context = contextBuilder.buildFinancialSummary(userId);
            
            // 2. Build prompt
            String prompt = promptBuilder.buildPrompt(category, userId, extras);
            
            // 3. Retrieve conversation memory
            List<AIConversationMessage> recentMessages = conversationRepository
                .findTop20ByUserIdOrderByTimestampDesc(userId);
            
            // 4. Select provider
            String mode = "AUTO"; // Could be made configurable per user
            String selectedProvider = providerRouter.selectProvider(mode, prompt).getClass().getSimpleName();
            
            // 5. Create AI request with context
            Map<String, Object> contextMap = new HashMap<>();
            contextMap.put("userId", userId);
            contextMap.put("recentMessages", recentMessages);
            contextMap.put("financialContext", context);
            
            AIRequest request = new AIRequest(prompt, contextMap, 1024);
            
            // 6. Generate response
            AIResponse response = aiRuntime.generate(request);
            
            // 7. Store conversation
            saveConversation(userId, userMessage, response.getText(), selectedProvider);
            
            // 8. Store embeddings for memory
            storeEmbeddings(userId, userMessage, response.getText());
            
            // 9. Log for observability
            log.info("AI Response generated - userId: {}, provider: {}, confidence: {}", 
                userId, selectedProvider, response.getConfidence());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error generating AI response for userId: {}", userId, e);
            Map<String, Object> fallbackMetadata = new HashMap<>();
            fallbackMetadata.put("provider", "fallback");
            return new AIResponse("I'm sorry, I'm having trouble processing your request right now.", 0.0, fallbackMetadata);
        }
    }
    
    /**
     * Generate prediction based on user behavior
     */
    public AIResponse generatePrediction(Long userId, String predictionType) {
        try {
            // Build prediction-specific context
            String context = contextBuilder.buildFinancialSummary(userId);
            
            // Build prediction prompt
            List<String> extras = List.of("prediction_type=" + predictionType);
            String prompt = promptBuilder.buildPrompt("PREDICTION", userId, extras);
            
            // Use SMART mode for predictions
            String selectedProvider = providerRouter.selectProvider("SMART", prompt).getClass().getSimpleName();
            
            Map<String, Object> contextMap = new HashMap<>();
            contextMap.put("userId", userId);
            contextMap.put("predictionType", predictionType);
            contextMap.put("financialContext", context);
            
            AIRequest request = new AIRequest(prompt, contextMap, 512);
            AIResponse response = aiRuntime.generate(request);
            
            log.info("Prediction generated - userId: {}, type: {}, provider: {}", 
                userId, predictionType, selectedProvider);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error generating prediction for userId: {}, type: {}", userId, predictionType, e);
            Map<String, Object> fallbackMetadata = new HashMap<>();
            fallbackMetadata.put("provider", "fallback");
            return new AIResponse("{\"error\": \"Prediction unavailable\"}", 0.0, fallbackMetadata);
        }
    }
    
    /**
     * Generate gamification message
     */
    public AIResponse generateGamificationMessage(Long userId, String eventType) {
        try {
            List<String> extras = List.of("event_type=" + eventType);
            String prompt = promptBuilder.buildPrompt("GAMIFICATION", userId, extras);
            
            String selectedProvider = providerRouter.selectProvider("FAST", prompt).getClass().getSimpleName();
            
            Map<String, Object> contextMap = new HashMap<>();
            contextMap.put("userId", userId);
            contextMap.put("eventType", eventType);
            
            AIRequest request = new AIRequest(prompt, contextMap, 256);
            AIResponse response = aiRuntime.generate(request);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error generating gamification message for userId: {}, event: {}", userId, eventType, e);
            Map<String, Object> fallbackMetadata = new HashMap<>();
            fallbackMetadata.put("provider", "fallback");
            return new AIResponse("Keep up the great work!", 0.5, fallbackMetadata);
        }
    }
    
    private void saveConversation(Long userId, String userMessage, String aiResponse, String provider) {
        try {
            // Save user message
            AIConversationMessage userMsg = AIConversationMessage.builder()
                .userId(userId)
                .role("USER")
                .message(userMessage)
                .timestamp(Instant.now())
                .build();
            conversationRepository.save(userMsg);
            
            // Save AI response
            AIConversationMessage aiMsg = AIConversationMessage.builder()
                .userId(userId)
                .role("AI")
                .message(aiResponse)
                .timestamp(Instant.now())
                .build();
            conversationRepository.save(aiMsg);
            
        } catch (Exception e) {
            log.error("Error saving conversation for userId: {}", userId, e);
        }
    }
    
    private void storeEmbeddings(Long userId, String userMessage, String aiResponse) {
        try {
            // Generate and store embeddings for both messages
            double[] userEmbedding = aiRuntime.embed(userMessage);
            double[] aiEmbedding = aiRuntime.embed(aiResponse);
            
            if (userEmbedding != null) {
                float[] userEmbeddingF = new float[userEmbedding.length];
                for (int i = 0; i < userEmbeddingF.length; i++) {
                    userEmbeddingF[i] = (float) userEmbedding[i];
                }
                vectorStoreService.storeEmbedding(userId, userEmbeddingF, "user_message", null);
            }
            
            if (aiEmbedding != null) {
                float[] aiEmbeddingF = new float[aiEmbedding.length];
                for (int i = 0; i < aiEmbeddingF.length; i++) {
                    aiEmbeddingF[i] = (float) aiEmbedding[i];
                }
                vectorStoreService.storeEmbedding(userId, aiEmbeddingF, "ai_response", null);
            }
            
        } catch (Exception e) {
            log.error("Error storing embeddings for userId: {}", userId, e);
        }
    }
}

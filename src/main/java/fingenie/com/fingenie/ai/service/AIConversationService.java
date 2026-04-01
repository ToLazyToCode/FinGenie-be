package fingenie.com.fingenie.ai.service;

import fingenie.com.fingenie.ai.core.AIRequest;
import fingenie.com.fingenie.ai.core.AIResponse;
import fingenie.com.fingenie.ai.core.AIRuntime;
import fingenie.com.fingenie.ai.dto.AIChatRequest;
import fingenie.com.fingenie.ai.dto.AIConversationResponse;
import fingenie.com.fingenie.ai.dto.AIConversationResponse.AIChatResult;
import fingenie.com.fingenie.ai.dto.AIConversationResponse.ConversationList;
import fingenie.com.fingenie.ai.dto.AIConversationResponse.MessageResponse;
import fingenie.com.fingenie.ai.entity.AIConversation;
import fingenie.com.fingenie.ai.entity.ChatMessage;
import fingenie.com.fingenie.ai.repository.AIChatMessageRepository;
import fingenie.com.fingenie.ai.repository.AIConversationEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIConversationService {

    private final AIConversationEntityRepository conversationRepository;
    private final AIChatMessageRepository messageRepository;
    private final AIRuntime aiRuntime;
    private final VectorStoreService vectorStoreService;
    
    private static final int MAX_CONTEXT_MESSAGES = 10;
    private static final int DEFAULT_MAX_TOKENS = 512;
    private static final Set<String> INVALID_CONTEXT_TOKENS =
            Set.of("undefined", "null", "none", "nan", "n/a");

    @Transactional
    public Long resolveConversationIdForChat(Long accountId, Long requestedConversationId, Boolean startNewConversation) {
        if (Boolean.TRUE.equals(startNewConversation) || requestedConversationId == null) {
            return createNewConversationInternal(accountId).getId();
        }

        return conversationRepository.findByIdAndAccountId(requestedConversationId, accountId)
                .map(AIConversation::getId)
                .orElseGet(() -> createNewConversationInternal(accountId).getId());
    }

    @Transactional
    public void persistGatewayChatResult(Long accountId, Long conversationId, AIChatResult result) {
        if (conversationId == null || result == null) {
            return;
        }

        AIConversation conversation = conversationRepository.findByIdAndAccountId(conversationId, accountId)
                .orElseGet(() -> createNewConversationInternal(accountId));

        ChatMessage savedUserMessage = saveGatewayMessage(conversation, result.getUserMessage());
        ChatMessage savedAiMessage = saveGatewayMessage(conversation, result.getAiMessage());

        if ("New Conversation".equals(conversation.getTitle()) && savedUserMessage != null) {
            String messageText = Optional.ofNullable(savedUserMessage.getText()).orElse("").trim();
            if (!messageText.isEmpty()) {
                String title = messageText.length() > 50 ? messageText.substring(0, 47) + "..." : messageText;
                conversation.setTitle(title);
            }
        }

        int addedTokens = 0;
        if (savedUserMessage != null && savedUserMessage.getTokenCount() != null) {
            addedTokens += savedUserMessage.getTokenCount();
        }
        if (savedAiMessage != null && savedAiMessage.getTokenCount() != null) {
            addedTokens += savedAiMessage.getTokenCount();
        }
        conversation.setTotalTokens(Optional.ofNullable(conversation.getTotalTokens()).orElse(0) + addedTokens);
        conversationRepository.save(conversation);
    }

    public AIChatResult buildFallbackChatResult(Long conversationId, String userMessageText) {
        return buildFallbackChatResult(conversationId, userMessageText, "en");
    }

    public AIChatResult buildFallbackChatResult(Long conversationId, String userMessageText, String languageCode) {
        return buildFallbackChatResult(conversationId, userMessageText, languageCode, null);
    }

    public AIChatResult buildFallbackChatResult(
            Long conversationId,
            String userMessageText,
            String languageCode,
            AIConversationResponse.FailureMetadata failure
    ) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        String safeUserMessage = Optional.ofNullable(userMessageText).orElse("").trim();
        if (safeUserMessage.isEmpty()) {
            safeUserMessage = "Hello";
        }

        MessageResponse userMessage = MessageResponse.builder()
                .id(null)
                .sender("USER")
                .text(safeUserMessage)
                .confidence(1.0)
                .intent("USER_INPUT")
                .modelUsed("none")
                .tokenCount(estimateTokenCount(safeUserMessage))
                .createdAt(now)
                .build();

        boolean isVietnamese = "vi".equalsIgnoreCase(languageCode);
        String fallbackText = isVietnamese
                ? "Tro ly AI tam thoi chua kha dung. Vui long thu lai sau it phut. "
                + "Ban van co the tiep tuc ghi nhan chi tieu va theo doi ke hoach tiet kiem hien tai."
                : "AI advisor is temporarily unavailable. Please try again in a moment. "
                + "You can still continue tracking expenses and following your current saving plan.";

        MessageResponse aiMessage = MessageResponse.builder()
                .id(null)
                .sender("AI")
                .text(fallbackText)
                .confidence(0.35)
                .intent("FALLBACK")
                .modelUsed("fallback")
                .tokenCount(estimateTokenCount(fallbackText))
                .createdAt(now)
                .build();

        return AIChatResult.builder()
                .conversationId(conversationId)
                .userMessage(userMessage)
                .aiMessage(aiMessage)
                .suggestions(isVietnamese
                        ? List.of(
                        "Cho toi xem tong quan chi tieu",
                        "Xem lai ke hoach tiet kiem thang",
                        "Goi y 1 khoan co the cat giam tuan nay"
                )
                        : List.of(
                        "Show me my spending breakdown",
                        "Review my monthly saving plan",
                        "Suggest one safe expense cut for this week"
                ))
                .detectedIntent("FALLBACK")
                .failure(failure)
                .build();
    }

    public String resolveLanguageCode(String requestedLanguage, String rawContext) {
        if (requestedLanguage != null && !requestedLanguage.isBlank()) {
            return toLanguageCode(requestedLanguage);
        }

        if (rawContext == null || rawContext.isBlank()) {
            return "en";
        }

        String language = "en";
        for (String line : rawContext.split("\\r?\\n")) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isEmpty() || !trimmed.contains("=")) {
                continue;
            }

            int separator = trimmed.indexOf('=');
            String key = trimmed.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            String value = normalizeContextValue(trimmed.substring(separator + 1));
            if (value.isEmpty()) {
                continue;
            }

            if ("app_language".equals(key)) {
                language = toLanguageCode(value);
                continue;
            }
            if ("preferred_response_language".equals(key)) {
                language = toLanguageCode(value);
            }
        }

        return language;
    }

    public String sanitizeAndEnrichContext(String rawContext, String languageCode) {
        LinkedHashSet<String> lines = new LinkedHashSet<>();
        if (rawContext != null && !rawContext.isBlank()) {
            for (String line : rawContext.split("\\r?\\n")) {
                String trimmed = line == null ? "" : line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                if (!trimmed.contains("=")) {
                    if (!isInvalidContextValue(trimmed)) {
                        lines.add(trimmed);
                    }
                    continue;
                }

                int separator = trimmed.indexOf('=');
                String key = trimmed.substring(0, separator).trim();
                String normalizedValue = normalizeContextValue(trimmed.substring(separator + 1));
                if (key.isEmpty() || normalizedValue.isEmpty()) {
                    continue;
                }
                lines.add(key + "=" + normalizedValue);
            }
        }

        String effectiveLanguageCode = toLanguageCode(languageCode);
        String languageName = "vi".equals(effectiveLanguageCode) ? "Vietnamese" : "English";
        lines.add("app_language=" + effectiveLanguageCode);
        lines.add("preferred_response_language=" + languageName);
        lines.add("response_language_strict=Reply only in " + languageName);

        return String.join("\n", lines);
    }

    public AIChatResult parseGatewayChatResult(
            Long resolvedConversationId,
            String userMessageText,
            Map<String, Object> aiResponse,
            String languageCode
    ) {
        if (aiResponse == null || aiResponse.isEmpty()) {
            throw new IllegalStateException("AI service returned empty response payload");
        }

        Long conversationId = readLong(aiResponse.get("conversationId"));
        if (conversationId == null) {
            conversationId = resolvedConversationId;
        }

        String safeUserMessage = Optional.ofNullable(userMessageText).orElse("").trim();
        if (safeUserMessage.isEmpty()) {
            safeUserMessage = "Hello";
        }

        MessageResponse userMessage = parseGatewayMessage(
                aiResponse.get("userMessage"),
                "USER",
                safeUserMessage,
                "USER_INPUT",
                "none"
        );

        MessageResponse aiMessage = parseGatewayMessage(
                aiResponse.get("aiMessage"),
                "AI",
                null,
                null,
                null
        );

        String aiText = aiMessage == null ? "" : Optional.ofNullable(aiMessage.getText()).orElse("").trim();
        if (aiText.isEmpty()) {
            // Accept a few common response field variants as graceful fallback mapping.
            aiText = firstNonBlank(
                    asCleanString(aiResponse.get("reply")),
                    asCleanString(aiResponse.get("response")),
                    asCleanString(aiResponse.get("text"))
            );
        }
        if (aiText.isEmpty()) {
            throw new IllegalStateException("AI response missing assistant text");
        }
        if (aiMessage == null) {
            aiMessage = MessageResponse.builder().build();
        }
        aiMessage.setSender(firstNonBlank(aiMessage.getSender(), "AI"));
        aiMessage.setText(aiText);
        if (aiMessage.getIntent() == null || aiMessage.getIntent().isBlank()) {
            aiMessage.setIntent(detectIntent(safeUserMessage));
        }
        if (aiMessage.getModelUsed() == null || aiMessage.getModelUsed().isBlank()) {
            aiMessage.setModelUsed("unknown");
        }
        if (aiMessage.getConfidence() == null) {
            aiMessage.setConfidence(0.75);
        }
        if (aiMessage.getTokenCount() == null || aiMessage.getTokenCount() <= 0) {
            aiMessage.setTokenCount(estimateTokenCount(aiText));
        }
        if (aiMessage.getCreatedAt() == null) {
            aiMessage.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        }

        if (userMessage.getTokenCount() == null || userMessage.getTokenCount() <= 0) {
            userMessage.setTokenCount(estimateTokenCount(userMessage.getText()));
        }
        if (userMessage.getCreatedAt() == null) {
            userMessage.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        }

        String detectedIntent = firstNonBlank(
                asCleanString(aiResponse.get("detectedIntent")),
                aiMessage.getIntent(),
                detectIntent(safeUserMessage)
        );

        List<String> suggestions = parseSuggestions(aiResponse.get("suggestions"));
        if (suggestions.isEmpty()) {
            suggestions = generateSuggestions(detectedIntent);
        } else if ("vi".equalsIgnoreCase(languageCode)) {
            // Keep suggestion language aligned when backend had to synthesize/normalize.
            suggestions = suggestions.stream()
                    .map(item -> item == null ? "" : item.trim())
                    .filter(item -> !item.isEmpty())
                    .collect(Collectors.toList());
        }

        return AIChatResult.builder()
                .conversationId(conversationId)
                .userMessage(userMessage)
                .aiMessage(aiMessage)
                .suggestions(suggestions)
                .detectedIntent(detectedIntent)
                .build();
    }

    public String summarizeGatewayResponse(Map<String, Object> aiResponse) {
        if (aiResponse == null) {
            return "response=null";
        }
        Object aiMessageObj = aiResponse.get("aiMessage");
        String aiText = "";
        if (aiMessageObj instanceof Map<?, ?> aiMap) {
            aiText = asCleanString(aiMap.get("text"));
        }
        return "keys=" + aiResponse.keySet()
                + ", conversationId=" + aiResponse.get("conversationId")
                + ", aiTextLength=" + aiText.length();
    }

    @Transactional
    public AIChatResult chat(Long accountId, AIChatRequest request) {
        AIConversation conversation;
        
        if (Boolean.TRUE.equals(request.getStartNewConversation()) || request.getConversationId() == null) {
            conversation = createNewConversationInternal(accountId);
        } else {
            conversation = conversationRepository.findByIdAndAccountId(request.getConversationId(), accountId)
                    .orElseGet(() -> createNewConversationInternal(accountId));
        }
        
        // Save user message
        ChatMessage userMessage = ChatMessage.builder()
                .conversation(conversation)
                .sender("USER")
                .text(request.getMessage())
                .build();
        messageRepository.save(userMessage);
        
        // Build context from recent messages
        List<ChatMessage> recentMessages = messageRepository.findRecentMessages(
                conversation.getId(), PageRequest.of(0, MAX_CONTEXT_MESSAGES));
        String contextPrompt = buildContextPrompt(recentMessages, request.getContext());
        
        // Generate AI response
        Map<String, Object> params = new HashMap<>();
        params.put("context", contextPrompt);
        AIRequest aiRequest = new AIRequest(request.getMessage(), params, DEFAULT_MAX_TOKENS);
        AIResponse aiResponse = aiRuntime.generate(aiRequest);
        
        // Detect intent
        String detectedIntent = detectIntent(request.getMessage());
        
        // Save AI response
        String modelUsed = aiResponse.getMetadata() != null ? 
                (String) aiResponse.getMetadata().getOrDefault("model", "default") : "default";
        
        ChatMessage aiMessage = ChatMessage.builder()
                .conversation(conversation)
                .sender("AI")
                .text(aiResponse.getText())
                .confidence(aiResponse.getConfidence())
                .intent(detectedIntent)
                .modelUsed(modelUsed)
                .tokenCount(estimateTokenCount(aiResponse.getText()))
                .build();
        messageRepository.save(aiMessage);
        
        // Update conversation metadata
        updateConversationMetadata(conversation, userMessage, aiMessage);
        
        // Store embeddings for RAG
        storeEmbeddings(accountId, userMessage, aiMessage);
        
        // Generate suggestions
        List<String> suggestions = generateSuggestions(detectedIntent);
        
        return AIChatResult.builder()
                .conversationId(conversation.getId())
                .userMessage(toMessageResponse(userMessage))
                .aiMessage(toMessageResponse(aiMessage))
                .suggestions(suggestions)
                .detectedIntent(detectedIntent)
                .build();
    }
    
    /**
     * Get conversations list for an account.
     * OSIV-SAFE: Maps to DTOs within transaction.
     */
    @Transactional(readOnly = true)
    public ConversationList getConversations(Long accountId, int limit) {
        List<AIConversation> conversations = conversationRepository
                .findActiveConversations(accountId, PageRequest.of(0, limit));
        
        List<AIConversationResponse> responses = conversations.stream()
                .map(this::toConversationResponse)
                .collect(Collectors.toList());
        
        long totalCount = conversationRepository.countByAccountId(accountId);
        
        return ConversationList.builder()
                .conversations(responses)
                .totalCount(totalCount)
                .build();
    }
    
    /**
     * Get a specific conversation with messages.
     * OSIV-SAFE: Maps to DTOs within transaction.
     */
    @Transactional(readOnly = true)
    public AIConversationResponse getConversation(Long accountId, Long conversationId) {
        AIConversation conversation = conversationRepository.findByIdAndAccountId(conversationId, accountId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        List<ChatMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        
        AIConversationResponse response = toConversationResponse(conversation);
        response.setRecentMessages(messages.stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList()));
        
        return response;
    }
    
    /**
     * Create a new conversation and return as DTO.
     * OSIV-SAFE: Returns DTO, not entity.
     */
    @Transactional
    public AIConversationResponse createNewConversation(Long accountId) {
        AIConversation saved = createNewConversationInternal(accountId);
        return toConversationResponse(saved);
    }
    
    /**
     * Internal method for creating conversation - returns entity for use within transactions.
     */
    private AIConversation createNewConversationInternal(Long accountId) {
        AIConversation conversation = AIConversation.builder()
                .accountId(accountId)
                .title("New Conversation")
                .isActive(true)
                .totalTokens(0)
                .build();
        return conversationRepository.save(conversation);
    }

    private ChatMessage saveGatewayMessage(AIConversation conversation, MessageResponse response) {
        if (response == null) {
            return null;
        }

        String text = Optional.ofNullable(response.getText()).orElse("").trim();
        if (text.isEmpty()) {
            return null;
        }

        ChatMessage entity = ChatMessage.builder()
                .conversation(conversation)
                .sender(Optional.ofNullable(response.getSender()).orElse("AI"))
                .text(text)
                .confidence(response.getConfidence())
                .intent(response.getIntent())
                .modelUsed(response.getModelUsed())
                .tokenCount(response.getTokenCount() != null ? response.getTokenCount() : estimateTokenCount(text))
                .build();
        return messageRepository.save(entity);
    }
    
    @Transactional
    public AIConversationResponse updateConversationTitle(Long accountId, Long conversationId, String title) {
        AIConversation conversation = conversationRepository.findByIdAndAccountId(conversationId, accountId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        conversation.setTitle(title);
        AIConversation saved = conversationRepository.save(conversation);
        return toConversationResponse(saved);
    }
    
    @Transactional
    public void archiveConversation(Long accountId, Long conversationId) {
        AIConversation conversation = conversationRepository.findByIdAndAccountId(conversationId, accountId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        conversation.setIsActive(false);
        conversationRepository.save(conversation);
    }
    
    @Transactional
    public void deleteConversation(Long accountId, Long conversationId) {
        AIConversation conversation = conversationRepository.findByIdAndAccountId(conversationId, accountId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        messageRepository.deleteByConversationId(conversationId);
        conversationRepository.delete(conversation);
    }
    
    @Transactional
    public void clearHistory(Long accountId) {
        List<AIConversation> conversations = conversationRepository.findByAccountIdOrderByUpdatedAtDesc(accountId);
        for (AIConversation conv : conversations) {
            messageRepository.deleteByConversationId(conv.getId());
        }
        conversationRepository.deleteByAccountId(accountId);
    }
    
    private String buildContextPrompt(List<ChatMessage> recentMessages, String additionalContext) {
        StringBuilder context = new StringBuilder();
        
        if (additionalContext != null && !additionalContext.isBlank()) {
            context.append("Additional context: ").append(additionalContext).append("\n\n");
        }
        
        if (!recentMessages.isEmpty()) {
            context.append("Recent conversation:\n");
            // Messages are in DESC order, reverse for chronological
            List<ChatMessage> chronological = new ArrayList<>(recentMessages);
            Collections.reverse(chronological);
            
            for (ChatMessage msg : chronological) {
                context.append(msg.getSender()).append(": ").append(msg.getText()).append("\n");
            }
        }
        
        return context.toString();
    }

    private MessageResponse parseGatewayMessage(
            Object rawMessage,
            String defaultSender,
            String defaultText,
            String defaultIntent,
            String defaultModel
    ) {
        Map<?, ?> messageMap = rawMessage instanceof Map<?, ?> map ? map : Collections.emptyMap();
        String text = firstNonBlank(
                asCleanString(messageMap.get("text")),
                asCleanString(messageMap.get("reply")),
                asCleanString(messageMap.get("response")),
                defaultText
        );

        Timestamp createdAt = parseTimestamp(messageMap.get("createdAt"));
        if (createdAt == null) {
            createdAt = new Timestamp(System.currentTimeMillis());
        }

        return MessageResponse.builder()
                .id(readLong(messageMap.get("id")))
                .sender(firstNonBlank(asCleanString(messageMap.get("sender")), defaultSender))
                .text(text)
                .confidence(readDouble(messageMap.get("confidence")))
                .intent(firstNonBlank(asCleanString(messageMap.get("intent")), defaultIntent))
                .modelUsed(firstNonBlank(asCleanString(messageMap.get("modelUsed")), defaultModel))
                .tokenCount(readInteger(messageMap.get("tokenCount")))
                .createdAt(createdAt)
                .build();
    }

    private List<String> parseSuggestions(Object rawSuggestions) {
        if (!(rawSuggestions instanceof Collection<?> collection)) {
            return new ArrayList<>();
        }
        List<String> items = new ArrayList<>();
        for (Object value : collection) {
            String text = asCleanString(value);
            if (!text.isEmpty()) {
                items.add(text);
            }
        }
        return items;
    }

    private Long readLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer readInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private Double readDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString().trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private Timestamp parseTimestamp(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp;
        }
        if (value instanceof Number number) {
            return new Timestamp(number.longValue());
        }
        if (value instanceof Date date) {
            return new Timestamp(date.getTime());
        }

        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Timestamp.from(Instant.parse(text));
        } catch (Exception ignored) {
            // Continue with lenient SQL timestamp parsing.
        }
        try {
            return Timestamp.valueOf(text.replace('T', ' ').replace("Z", ""));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String asCleanString(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString().trim();
        if (isInvalidContextValue(text)) {
            return "";
        }
        return text;
    }

    private boolean isInvalidContextValue(String value) {
        if (value == null) {
            return true;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() || INVALID_CONTEXT_TOKENS.contains(normalized);
    }

    private String normalizeContextValue(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'"))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        if (isInvalidContextValue(normalized)) {
            return "";
        }
        return normalized;
    }

    private String toLanguageCode(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("vi") || normalized.contains("vietnam")) {
            return "vi";
        }
        return "en";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
    
    private String detectIntent(String message) {
        String lower = message.toLowerCase();
        
        if (lower.contains("spend") || lower.contains("expense") || lower.contains("cost")) {
            return "SPENDING_QUERY";
        }
        if (lower.contains("save") || lower.contains("saving") || lower.contains("goal")) {
            return "SAVINGS_QUERY";
        }
        if (lower.contains("budget") || lower.contains("limit")) {
            return "BUDGET_QUERY";
        }
        if (lower.contains("predict") || lower.contains("forecast") || lower.contains("estimate")) {
            return "PREDICTION_QUERY";
        }
        if (lower.contains("advice") || lower.contains("tip") || lower.contains("recommend")) {
            return "ADVICE_QUERY";
        }
        if (lower.contains("category") || lower.contains("classify")) {
            return "CATEGORIZATION_QUERY";
        }
        
        return "GENERAL_QUERY";
    }
    
    private List<String> generateSuggestions(String intent) {
        return switch (intent) {
            case "SPENDING_QUERY" -> List.of(
                "Show me my spending breakdown",
                "What are my top spending categories?",
                "How can I reduce expenses?"
            );
            case "SAVINGS_QUERY" -> List.of(
                "How much should I save monthly?",
                "Create a savings goal",
                "Track my savings progress"
            );
            case "BUDGET_QUERY" -> List.of(
                "Set a budget for this month",
                "Which categories exceed budget?",
                "Adjust my budget limits"
            );
            case "PREDICTION_QUERY" -> List.of(
                "Predict next month's expenses",
                "When will I reach my savings goal?",
                "What's my projected balance?"
            );
            case "ADVICE_QUERY" -> List.of(
                "Give me money-saving tips",
                "How to improve my financial health?",
                "Suggest ways to invest"
            );
            default -> List.of(
                "Show my financial summary",
                "What did I spend on today?",
                "Check my savings progress"
            );
        };
    }
    
    private void updateConversationMetadata(AIConversation conversation, 
                                            ChatMessage userMessage, 
                                            ChatMessage aiMessage) {
        // Update title if first message
        if (conversation.getTitle().equals("New Conversation")) {
            String title = userMessage.getText();
            if (title.length() > 50) {
                title = title.substring(0, 47) + "...";
            }
            conversation.setTitle(title);
        }
        
        // Update token count
        int newTokens = (userMessage.getTokenCount() != null ? userMessage.getTokenCount() : 0) +
                       (aiMessage.getTokenCount() != null ? aiMessage.getTokenCount() : 0);
        conversation.setTotalTokens(conversation.getTotalTokens() + newTokens);
        
        conversationRepository.save(conversation);
    }
    
    private void storeEmbeddings(Long accountId, ChatMessage userMessage, ChatMessage aiMessage) {
        try {
            double[] userEmbed = aiRuntime.embed(userMessage.getText());
            if (userEmbed != null) {
                float[] embeddingF = new float[userEmbed.length];
                for (int i = 0; i < userEmbed.length; i++) embeddingF[i] = (float) userEmbed[i];
                vectorStoreService.storeEmbedding(accountId, embeddingF, "chat_user", userMessage.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to store user message embedding", e);
        }
    }
    
    private int estimateTokenCount(String text) {
        // Rough estimate: ~4 chars per token
        return text != null ? text.length() / 4 : 0;
    }
    
    private AIConversationResponse toConversationResponse(AIConversation conversation) {
        return AIConversationResponse.builder()
                .id(conversation.getId())
                .accountId(conversation.getAccountId())
                .title(conversation.getTitle())
                .isActive(conversation.getIsActive())
                .contextSummary(conversation.getContextSummary())
                .totalTokens(conversation.getTotalTokens())
                .messageCount((int) messageRepository.countByConversationId(conversation.getId()))
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
    
    private MessageResponse toMessageResponse(ChatMessage message) {
        return MessageResponse.builder()
                .id(message.getId())
                .sender(message.getSender())
                .text(message.getText())
                .confidence(message.getConfidence())
                .intent(message.getIntent())
                .modelUsed(message.getModelUsed())
                .tokenCount(message.getTokenCount())
                .createdAt(message.getCreatedAt())
                .build();
    }
}

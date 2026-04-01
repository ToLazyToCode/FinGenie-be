package fingenie.com.fingenie.ai.controller;

import fingenie.com.fingenie.ai.client.AIClient;
import fingenie.com.fingenie.ai.client.AIClientException;
import fingenie.com.fingenie.ai.client.dto.ChatRequest;
import fingenie.com.fingenie.ai.dto.AIChatRequest;
import fingenie.com.fingenie.ai.dto.AIConversationResponse;
import fingenie.com.fingenie.ai.dto.AIConversationResponse.AIChatResult;
import fingenie.com.fingenie.ai.dto.AIConversationResponse.ConversationList;
import fingenie.com.fingenie.ai.service.AIConversationService;
import fingenie.com.fingenie.entitlement.EntitlementService;
import fingenie.com.fingenie.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/ai/conversations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Conversations", description = "AI chat conversation management APIs")
public class AIConversationController {

    private final AIClient aiClient;
    private final AIConversationService conversationService;
    private final EntitlementService entitlementService;

    @PostMapping("/chat")
    @Operation(summary = "Send a message to AI and get a response")
    public ResponseEntity<AIChatResult> chat(@Valid @RequestBody AIChatRequest request) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        entitlementService.assertFeature(accountId, "ai.chat");
        entitlementService.assertAiChatQuota(accountId);
        Long resolvedConversationId = conversationService.resolveConversationIdForChat(
                accountId,
                request.getConversationId(),
                request.getStartNewConversation()
        );
        String languageCode = conversationService.resolveLanguageCode(request.getLanguage(), request.getContext());
        String sanitizedContext = conversationService.sanitizeAndEnrichContext(request.getContext(), languageCode);

        ChatRequest payload = ChatRequest.builder()
                .accountId(accountId)
                .conversationId(resolvedConversationId)
                .message(request.getMessage())
                .context(sanitizedContext)
                .language(languageCode)
                .startNewConversation(false)
                .build();

        AIChatResult result;
        Map<String, Object> aiResponse = null;
        try {
            aiResponse = aiClient.chat(payload);
            result = conversationService.parseGatewayChatResult(
                    resolvedConversationId,
                    request.getMessage(),
                    aiResponse,
                    languageCode
            );
        } catch (Exception ex) {
            AIConversationResponse.FailureMetadata failure = toFailureMetadata(ex);
            log.warn(
                    "AI gateway fallback triggered accountId={} conversationId={} language={} reasonType={} path={} elapsedMs={} timeoutMs={} message={} responseSummary={}",
                    accountId,
                    resolvedConversationId,
                    languageCode,
                    failure.getReasonType(),
                    failure.getPath(),
                    failure.getElapsedMs(),
                    failure.getTimeoutMs(),
                    failure.getMessage(),
                    conversationService.summarizeGatewayResponse(aiResponse)
            );
            log.debug("AI gateway fallback stacktrace accountId={} conversationId={}", accountId, resolvedConversationId, ex);
            result = conversationService.buildFallbackChatResult(
                    resolvedConversationId,
                    request.getMessage(),
                    languageCode,
                    failure
            );
        }

        if (result.getConversationId() == null || !result.getConversationId().equals(resolvedConversationId)) {
            result.setConversationId(resolvedConversationId);
        }
        conversationService.persistGatewayChatResult(accountId, resolvedConversationId, result);

        return ResponseEntity.ok(result);
    }

    private AIConversationResponse.FailureMetadata toFailureMetadata(Throwable throwable) {
        Throwable root = throwable;
        while (root != null && root.getCause() != null) {
            root = root.getCause();
        }

        AIClientException aiClientException = findAIClientException(throwable);
        if (aiClientException != null) {
            return AIConversationResponse.FailureMetadata.builder()
                    .source("fallback")
                    .reasonType(aiClientException.getFailureType().name())
                    .path(aiClientException.getPath())
                    .elapsedMs(aiClientException.getElapsedMs())
                    .timeoutMs(aiClientException.getTimeoutMs())
                    .message(aiClientException.getMessage())
                    .build();
        }

        return AIConversationResponse.FailureMetadata.builder()
                .source("fallback")
                .reasonType(root == null ? "UNKNOWN" : root.getClass().getSimpleName())
                .path("/ai/chat")
                .elapsedMs(null)
                .timeoutMs(null)
                .message(root == null ? "unknown" : Optional.ofNullable(root.getMessage()).orElse("no-message"))
                .build();
    }

    private AIClientException findAIClientException(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null) {
            if (cursor instanceof AIClientException aiClientException) {
                return aiClientException;
            }
            cursor = cursor.getCause();
        }
        return null;
    }

    @GetMapping
    @Operation(summary = "Get list of conversations")
    public ResponseEntity<ConversationList> getConversations(
            @RequestParam(defaultValue = "20") int limit) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(conversationService.getConversations(accountId, limit));
    }

    @GetMapping("/{conversationId}")
    @Operation(summary = "Get a specific conversation with messages")
    public ResponseEntity<AIConversationResponse> getConversation(
            @PathVariable Long conversationId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(conversationService.getConversation(accountId, conversationId));
    }

    @PostMapping
    @Operation(summary = "Create a new conversation")
    public ResponseEntity<AIConversationResponse> createConversation() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(conversationService.createNewConversation(accountId));
    }

    @PutMapping("/{conversationId}/title")
    @Operation(summary = "Update conversation title")
    public ResponseEntity<AIConversationResponse> updateTitle(
            @PathVariable Long conversationId,
            @RequestParam String title) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(conversationService.updateConversationTitle(accountId, conversationId, title));
    }

    @PostMapping("/{conversationId}/archive")
    @Operation(summary = "Archive a conversation")
    public ResponseEntity<Void> archiveConversation(@PathVariable Long conversationId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        conversationService.archiveConversation(accountId, conversationId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{conversationId}")
    @Operation(summary = "Delete a conversation")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long conversationId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        conversationService.deleteConversation(accountId, conversationId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/clear")
    @Operation(summary = "Clear all conversation history")
    public ResponseEntity<Void> clearHistory() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        conversationService.clearHistory(accountId);
        return ResponseEntity.noContent().build();
    }
}

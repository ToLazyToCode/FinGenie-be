package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.*;
import fingenie.com.fingenie.service.FriendChatService;
import fingenie.com.fingenie.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Friend Chat", description = "Friend messaging endpoints")
public class FriendChatController {

    private final FriendChatService chatService;

    @GetMapping("/conversations")
    @Operation(summary = "Get conversations", description = "Get all conversations for current user")
    public ResponseEntity<List<ConversationResponse>> getConversations() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(chatService.getConversations(accountId));
    }

    @PostMapping("/conversations/with/{friendId}")
    @Operation(summary = "Start conversation", description = "Get or create conversation with a friend")
    public ResponseEntity<ConversationResponse> getOrCreateConversation(@PathVariable Long friendId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(chatService.getOrCreateConversation(accountId, friendId));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Get messages", description = "Get paginated messages for a conversation")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(chatService.getMessages(accountId, conversationId, page, size));
    }

    @PostMapping("/messages")
    @Operation(summary = "Send message", description = "Send a message to a conversation")
    public ResponseEntity<ChatMessageResponse> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(chatService.sendMessage(accountId, request));
    }

    @PostMapping("/conversations/{conversationId}/read")
    @Operation(summary = "Mark as read", description = "Mark all messages in conversation as read")
    public ResponseEntity<Map<String, Integer>> markAsRead(@PathVariable Long conversationId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        int count = chatService.markAsRead(accountId, conversationId);
        return ResponseEntity.ok(Map.of("markedCount", count));
    }

    @GetMapping("/unread/count")
    @Operation(summary = "Total unread count", description = "Get total unread message count across all conversations")
    public ResponseEntity<Map<String, Long>> getTotalUnreadCount() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        long count = chatService.getTotalUnreadCount(accountId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}

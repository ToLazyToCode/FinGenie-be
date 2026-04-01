package fingenie.com.fingenie.service;

import fingenie.com.fingenie.dto.*;
import fingenie.com.fingenie.entity.ChatMessage;
import fingenie.com.fingenie.entity.Conversation;
import fingenie.com.fingenie.repository.ChatMessageRepository;
import fingenie.com.fingenie.repository.ConversationRepository;
import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.repository.AccountRepository;
import fingenie.com.fingenie.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendChatService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final FriendshipRepository friendshipRepository;
    private final AccountRepository accountRepository;
    private final NotificationService notificationService;

    /**
     * Get all conversations for a user.
     */
    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(Long accountId) {
        List<Conversation> conversations = conversationRepository.findByParticipantId(accountId);
        
        if (conversations.isEmpty()) {
            return Collections.emptyList();
        }

        // Get unread counts
        List<Long> conversationIds = conversations.stream().map(Conversation::getId).toList();
        Map<Long, Long> unreadCounts = new HashMap<>();
        messageRepository.countUnreadByConversations(conversationIds, accountId)
                .forEach(row -> unreadCounts.put((Long) row[0], (Long) row[1]));

        // Get partner account info
        Set<Long> partnerIds = new HashSet<>();
        for (Conversation c : conversations) {
            c.getParticipantIds().stream()
                    .filter(id -> !id.equals(accountId))
                    .forEach(partnerIds::add);
        }

        Map<Long, Account> partners = accountRepository.findAllById(partnerIds).stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));

        return conversations.stream()
                .map(c -> toConversationResponse(c, accountId, partners, unreadCounts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    /**
     * Get or create a conversation with a friend.
     */
    @Transactional
    public ConversationResponse getOrCreateConversation(Long accountId, Long friendId) {
        // Verify friendship exists
        boolean areFriends = friendshipRepository.areFriends(accountId, friendId);
        if (!areFriends) {
            throw new IllegalArgumentException("You can only chat with friends");
        }

        // Check if conversation exists
        Optional<Conversation> existing = conversationRepository.findByParticipants(accountId, friendId);
        if (existing.isPresent()) {
            Account partner = accountRepository.findById(friendId).orElse(null);
            return toConversationResponse(existing.get(), accountId, 
                    partner != null ? Map.of(friendId, partner) : Collections.emptyMap(), 0L);
        }

        // Create new conversation
        Conversation conversation = Conversation.builder()
                .participantIds(new HashSet<>(Set.of(accountId, friendId)))
                .isActive(true)
                .build();
        conversation = conversationRepository.save(conversation);

        Account partner = accountRepository.findById(friendId).orElse(null);
        return toConversationResponse(conversation, accountId,
                partner != null ? Map.of(friendId, partner) : Collections.emptyMap(), 0L);
    }

    /**
     * Get messages for a conversation.
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long accountId, Long conversationId, int page, int size) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        if (!conversation.hasParticipant(accountId)) {
            throw new SecurityException("Not a participant in this conversation");
        }

        // Get sender names
        Map<Long, String> senderNames = accountRepository.findAllById(conversation.getParticipantIds())
                .stream()
                .collect(Collectors.toMap(
                        Account::getId,
                        account -> resolveAccountName(account, "Unknown"),
                        (existing, replacement) -> existing
                ));

        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(page, size))
                .map(m -> toMessageResponse(m, accountId, senderNames.get(m.getSenderId())))
                .getContent();
    }

    /**
     * Send a message.
     */
    @Transactional
    public ChatMessageResponse sendMessage(Long accountId, SendMessageRequest request) {
        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        if (!conversation.hasParticipant(accountId)) {
            throw new SecurityException("Not a participant in this conversation");
        }

        ChatMessage.MessageType messageType = ChatMessage.MessageType.TEXT;
        if (request.getMessageType() != null) {
            messageType = ChatMessage.MessageType.valueOf(request.getMessageType());
        }

        ChatMessage message = ChatMessage.builder()
                .conversationId(conversation.getId())
                .senderId(accountId)
                .content(request.getContent())
                .messageType(messageType)
                .attachmentUrl(request.getAttachmentUrl())
                .isRead(false)
                .build();
        message = messageRepository.save(message);

        // Update conversation
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setLastMessagePreview(truncatePreview(request.getContent()));
        conversationRepository.save(conversation);

        // Notify other participants
        Account sender = accountRepository.findById(accountId).orElse(null);
        String senderName = resolveAccountName(sender, "Someone");

        for (Long participantId : conversation.getParticipantIds()) {
            if (!participantId.equals(accountId)) {
                notificationService.createSimpleNotification(
                        participantId,
                        NotificationService.TYPE_FRIEND_MESSAGE,
                        "New message from " + senderName,
                        truncatePreview(request.getContent())
                );
            }
        }

        return toMessageResponse(message, accountId, senderName);
    }

    /**
     * Mark messages as read.
     */
    @Transactional
    public int markAsRead(Long accountId, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        if (!conversation.hasParticipant(accountId)) {
            throw new SecurityException("Not a participant in this conversation");
        }

        return messageRepository.markAsRead(conversationId, accountId);
    }

    /**
     * Get total unread message count.
     */
    @Transactional(readOnly = true)
    public long getTotalUnreadCount(Long accountId) {
        List<Conversation> conversations = conversationRepository.findByParticipantId(accountId);
        if (conversations.isEmpty()) return 0;

        List<Long> conversationIds = conversations.stream().map(Conversation::getId).toList();
        return messageRepository.countUnreadByConversations(conversationIds, accountId)
                .stream()
                .mapToLong(row -> (Long) row[1])
                .sum();
    }

    // ==================== Private Helpers ====================

    private ConversationResponse toConversationResponse(Conversation conversation, Long currentUserId,
                                                        Map<Long, Account> partners, Long unreadCount) {
        Long partnerId = conversation.getParticipantIds().stream()
                .filter(id -> !id.equals(currentUserId))
                .findFirst()
                .orElse(null);

        Account partner = partnerId != null ? partners.get(partnerId) : null;

        return ConversationResponse.builder()
                .id(conversation.getId())
                .participantIds(conversation.getParticipantIds())
                .partnerName(resolveAccountName(partner, "Unknown"))
                .partnerAvatar(partner != null ? partner.getAvatarUrl() : null)
                .lastMessagePreview(conversation.getLastMessagePreview())
                .lastMessageAt(conversation.getLastMessageAt())
                .unreadCount(unreadCount)
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message, Long currentUserId, String senderName) {
        ChatMessage.MessageType messageType = message.getMessageType() != null
                ? message.getMessageType()
                : ChatMessage.MessageType.TEXT;
        if (message.getMessageType() == null) {
            log.warn("Chat message {} in conversation {} has null messageType. Falling back to TEXT.",
                    message.getId(), message.getConversationId());
        }

        return ChatMessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .senderName(resolveDisplayName(senderName, "Unknown"))
                .content(message.getContent())
                .messageType(messageType.name())
                .isRead(Boolean.TRUE.equals(message.getIsRead()))
                .attachmentUrl(message.getAttachmentUrl())
                .isMine(message.getSenderId().equals(currentUserId))
                .createdAt(message.getCreatedAt())
                .readAt(message.getReadAt())
                .build();
    }

    private String truncatePreview(String content) {
        if (content == null) return null;
        return content.length() > 100 ? content.substring(0, 97) + "..." : content;
    }

    private String resolveAccountName(Account account, String fallback) {
        return account == null ? fallback : resolveDisplayName(account.getName(), fallback);
    }

    private String resolveDisplayName(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}

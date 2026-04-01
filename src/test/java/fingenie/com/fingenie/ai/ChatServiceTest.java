package fingenie.com.fingenie.ai;

import fingenie.com.fingenie.ai.core.AIRuntime;
import fingenie.com.fingenie.ai.core.AIRequest;
import fingenie.com.fingenie.ai.core.AIResponse;
import fingenie.com.fingenie.ai.dto.AIChatRequest;
import fingenie.com.fingenie.ai.dto.AIConversationResponse.AIChatResult;
import fingenie.com.fingenie.ai.entity.AIConversation;
import fingenie.com.fingenie.ai.entity.ChatMessage;
import fingenie.com.fingenie.ai.repository.AIChatMessageRepository;
import fingenie.com.fingenie.ai.repository.AIConversationEntityRepository;
import fingenie.com.fingenie.ai.service.AIConversationService;
import fingenie.com.fingenie.ai.service.VectorStoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatServiceTest {

    @Mock
    AIConversationEntityRepository conversationRepository;
    @Mock
    AIChatMessageRepository messageRepository;
    @Mock
    AIRuntime aiRuntime;
    @Mock
    VectorStoreService vectorStoreService;

    @InjectMocks
    AIConversationService chatService;

    @Test
    public void testSendMessageSavesUserAndAIMessages() {
        // Setup mocks
        when(conversationRepository.save(any(AIConversation.class))).thenAnswer(inv -> {
            AIConversation conversation = inv.getArgument(0);
            conversation.setId(100L);
            return conversation;
        });
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage message = inv.getArgument(0);
            if ("USER".equals(message.getSender())) {
                message.setId(101L);
            } else {
                message.setId(102L);
            }
            return message;
        });
        when(messageRepository.findRecentMessages(anyLong(), any())).thenReturn(List.of());
        when(aiRuntime.generate(any(AIRequest.class)))
                .thenReturn(new AIResponse("Hi there!", 0.95, new HashMap<>(Map.of("model", "mock-model"))));
        when(aiRuntime.embed(anyString())).thenReturn(new double[]{0.1, 0.2, 0.3});
        doNothing().when(vectorStoreService).storeEmbedding(anyLong(), any(float[].class), anyString(), anyLong());

        // Execute
        AIChatRequest req = AIChatRequest.builder()
                .message("Hello AI")
                .context("User profile context")
                .build();
        AIChatResult resp = chatService.chat(1L, req);

        // Verify response
        assertNotNull(resp);
        assertEquals(100L, resp.getConversationId());
        assertNotNull(resp.getAiMessage());
        assertEquals("Hi there!", resp.getAiMessage().getText());
        assertEquals(0.95, resp.getAiMessage().getConfidence(), 0.01);
        assertNotNull(resp.getSuggestions());
        assertFalse(resp.getSuggestions().isEmpty());

        // Verify interactions
        verify(conversationRepository, times(2)).save(any(AIConversation.class));
        verify(messageRepository, times(2)).save(any(ChatMessage.class));
        verify(messageRepository, times(1)).findRecentMessages(anyLong(), any());
        verify(aiRuntime, times(1)).generate(any(AIRequest.class));
        verify(vectorStoreService, times(1)).storeEmbedding(anyLong(), any(float[].class), anyString(), anyLong());
    }
}

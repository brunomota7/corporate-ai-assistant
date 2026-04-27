package br.com.api_core.modules.chat;

import br.com.api_core.client.AiServiceClient;
import br.com.api_core.domain.User;
import br.com.api_core.domain.enums.MessageRole;
import br.com.api_core.domain.enums.Role;
import br.com.api_core.infra.exception.AiServiceUnavailableException;
import br.com.api_core.modules.audit.AuditService;
import br.com.api_core.modules.chat.dto.ChatRequestDTO;
import br.com.api_core.modules.chat.dto.ChatResponseDTO;
import br.com.api_core.modules.chat.dto.MessageDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatHistoryService chatHistoryService;

    @Mock
    private AiServiceClient aiServiceClient;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ChatService chatService;

    private User user;
    private static final String IP = "127.0.0.1";

    @BeforeEach
    void setUp() {
        user = new User();
        user.setName("Jhon");
        user.setEmail("jhon@test.com");
        user.setRole(Role.ROLE_USER);

        try {
            var field = user.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, UUID.randomUUID());
        } catch (Exception ignored) {}
    }

    @Test
    void sendMessage_generatesSessionId_whenNotProvided() {
        ChatRequestDTO dto = new ChatRequestDTO("Qual o estoque?", null);

        when(chatHistoryService.getHistory(anyString(), anyString()))
                .thenReturn(List.of());
        when(aiServiceClient.search(anyString()))
                .thenReturn(List.of());
        when(aiServiceClient.chat(anyList(), anyList()))
                .thenReturn(new AiServiceClient.ChatCompletion("Resposta", 10));

        ChatResponseDTO response = chatService.sendMessage(dto, user, IP);

        assertNotNull(response.sessionId());
        assertFalse(response.sessionId().isBlank());
    }

    @Test
    void sendMessage_usesProvidedSessionId_whenPresent() {
        String existingSessionId = UUID.randomUUID().toString();
        ChatRequestDTO dto = new ChatRequestDTO("Qual o preço?", existingSessionId);

        when(chatHistoryService.getHistory(anyString(), eq(existingSessionId)))
                .thenReturn(List.of());
        when(aiServiceClient.search(anyString()))
                .thenReturn(List.of());
        when(aiServiceClient.chat(anyList(), anyList()))
                .thenReturn(new AiServiceClient.ChatCompletion("Resposta", 10));

        ChatResponseDTO response = chatService.sendMessage(dto, user, IP);

        assertEquals(existingSessionId, response.sessionId());
    }

    @Test
    void sendMessage_returnsAnswerAndToken_onSuccess() {
        ChatRequestDTO dto = new ChatRequestDTO("Olá", null);

        when(chatHistoryService.getHistory(anyString(), anyString()))
                .thenReturn(List.of());
        when(aiServiceClient.search(anyString()))
                .thenReturn(List.of("chunk relevante"));
        when(aiServiceClient.chat(anyList(), anyList()))
                .thenReturn(new AiServiceClient.ChatCompletion("Olá! Como posso ajudar?", 25));

        ChatResponseDTO response = chatService.sendMessage(dto, user, IP);

        assertEquals("Olá! Como posso ajudar?", response.answer());
        assertEquals(25, response.tokensUsed());
    }

    @Test
    void sendMessage_savesUpdatedHistoryInRedis() {
        ChatRequestDTO dto = new ChatRequestDTO("Pergunta", null);
        List<MessageDTO> existingHistory = List.of(
                new MessageDTO(MessageRole.USER, "Mensagem anterior"),
                new MessageDTO(MessageRole.ASSISTANT, "Resposta anterior")
        );

        when(chatHistoryService.getHistory(anyString(), anyString()))
                .thenReturn(existingHistory);
        when(aiServiceClient.search(anyString()))
                .thenReturn(List.of());
        when(aiServiceClient.chat(anyList(), anyList()))
                .thenReturn(new AiServiceClient.ChatCompletion("Nova resposta", 15));

        chatService.sendMessage(dto, user, IP);

        ArgumentCaptor<List<MessageDTO>> historyCaptor = ArgumentCaptor.forClass(List.class);
        verify(chatHistoryService).saveHistory(anyString(), anyString(), historyCaptor.capture());

        List<MessageDTO> savedHistory = historyCaptor.getValue();
        assertEquals(4, savedHistory.size());
        assertEquals(MessageRole.USER, savedHistory.get(2).role());
        assertEquals("Pergunta", savedHistory.get(2).content());
        assertEquals(MessageRole.ASSISTANT, savedHistory.get(3).role());
        assertEquals("Nova resposta", savedHistory.get(3).content());
    }

    @Test
    void sendMessage_callsAuditService_afterSuccess() {
        ChatRequestDTO dto = new ChatRequestDTO("Pergunta auditada", null);

        when(chatHistoryService.getHistory(anyString(), anyString()))
                .thenReturn(List.of());
        when(aiServiceClient.search(anyString()))
                .thenReturn(List.of());
        when(aiServiceClient.chat(anyList(), anyList()))
                .thenReturn(new AiServiceClient.ChatCompletion("Resposta auditada", 20));

        chatService.sendMessage(dto, user, IP);

        verify(auditService).save(
                eq(user),
                anyString(),
                eq("Pergunta auditada"),
                eq("Resposta auditada"),
                eq(20),
                anyInt(),
                eq(IP)
        );
    }

    @Test
    void sendMessage_doesNotCallAudit_whenAiServiceFails() {
        ChatRequestDTO dto = new ChatRequestDTO("Pergunta", null);

        when(chatHistoryService.getHistory(anyString(), anyString()))
                .thenReturn(List.of());
        when(aiServiceClient.search(anyString()))
                .thenThrow(new AiServiceUnavailableException("ai-service down", new RuntimeException()));

        assertThrows(AiServiceUnavailableException.class,
                () -> chatService.sendMessage(dto, user, IP));

        verifyNoInteractions(auditService);
    }

    @Test
    void sendMessage_continuesNormally_whenAuditFails() {
        ChatRequestDTO dto = new ChatRequestDTO("Pergunta", null);

        when(chatHistoryService.getHistory(anyString(), anyString()))
                .thenReturn(List.of());
        when(aiServiceClient.search(anyString()))
                .thenReturn(List.of());
        when(aiServiceClient.chat(anyList(), anyList()))
                .thenReturn(new AiServiceClient.ChatCompletion("Resposta", 10));
        doThrow(new RuntimeException("DB fora"))
                .when(auditService).save(any(), any(), any(), any(), any(), any(), any());

        ChatResponseDTO response = chatService.sendMessage(dto, user, IP);

        assertNotNull(response);
        assertEquals("Resposta", response.answer());
    }
}
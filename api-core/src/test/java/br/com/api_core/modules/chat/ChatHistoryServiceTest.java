package br.com.api_core.modules.chat;

import br.com.api_core.domain.enums.MessageRole;
import br.com.api_core.modules.chat.dto.MessageDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatHistoryServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private ChatHistoryService chatHistoryService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void getHistory_returnEmpty_whenKeyNotFound() {
        when(valueOps.get("chat:user:session")).thenReturn(null);

        List<MessageDTO> history = chatHistoryService.getHistory("user", "session");

        assertTrue(history.isEmpty());
    }

    @Test
    void getHistory_returnsMessages_whenKeyExists() throws Exception {
        List<MessageDTO> messages = List.of(
                new MessageDTO(MessageRole.USER, "Olá"),
                new MessageDTO(MessageRole.ASSISTANT, "Olá! Como posso ajudar?")
        );

        String json = new ObjectMapper().writeValueAsString(messages);
        when(valueOps.get("chat:user:session")).thenReturn(json);

        List<MessageDTO> result = chatHistoryService.getHistory("user", "session");

        assertEquals(2, result.size());
        assertEquals(MessageRole.USER, result.get(0).role());
        assertEquals("Olá", result.get(0).content());
    }

    @Test
    void saveHistory_persistsJsonWithTTL() throws Exception {
        List<MessageDTO> messages = List.of(new MessageDTO(MessageRole.USER, "Hello"));

        chatHistoryService.saveHistory("user", "session", messages);

        verify(valueOps).set(eq("chat:user:session"), anyString(), eq(7200L), eq(TimeUnit.SECONDS));
    }
}
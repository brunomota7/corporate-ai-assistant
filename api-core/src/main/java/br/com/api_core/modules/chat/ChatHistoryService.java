package br.com.api_core.modules.chat;

import br.com.api_core.modules.chat.dto.MessageDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static reactor.netty.http.HttpConnectionLiveness.log;

@Service
public class ChatHistoryService {

    private static final int TTL_SECONDS = 7200;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RedisTemplate<String, String> redisTemplate;

    public ChatHistoryService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String buildKey(String userId, String sessionId) {
        return String.format("chat:%s:%s", userId, sessionId);
    }

    public List<MessageDTO> getHistory(String userId, String sessionId) {
        String key = buildKey(userId, sessionId);
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            return new ArrayList<>();
        }

        try {
            return OBJECT_MAPPER.readValue(
                    json,
                    new TypeReference<List<MessageDTO>>() {}
            );
        } catch (JsonProcessingException e) {
            log.error("Error deserializing Redis history. Key: {}", key, e);
            return new ArrayList<>();
        }
    }

    public void saveHistory(String userId, String sessionId, List<MessageDTO> messages) {
        String key = buildKey(userId, sessionId);
        try {
            String json = OBJECT_MAPPER.writeValueAsString(messages);
            redisTemplate.opsForValue().set(key, json, TTL_SECONDS, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar histórico de chat", e);
        }
    }

}


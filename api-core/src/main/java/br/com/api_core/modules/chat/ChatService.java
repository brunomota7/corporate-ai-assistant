package br.com.api_core.modules.chat;

import br.com.api_core.client.AiServiceClient;
import br.com.api_core.domain.User;
import br.com.api_core.domain.enums.MessageRole;
import br.com.api_core.modules.audit.AuditService;
import br.com.api_core.modules.chat.dto.ChatRequestDTO;
import br.com.api_core.modules.chat.dto.ChatResponseDTO;
import br.com.api_core.modules.chat.dto.MessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ChatService {

    private final ChatHistoryService chatHistoryService;
    private final AiServiceClient aiServiceClient;
    private final AuditService auditService;

    public ChatService(ChatHistoryService chatHistoryService,
                       AiServiceClient aiServiceClient,
                       AuditService auditService) {
        this.chatHistoryService = chatHistoryService;
        this.aiServiceClient = aiServiceClient;
        this.auditService = auditService;
    }

    public ChatResponseDTO sendMessage(ChatRequestDTO dto, User user, String ipAddress) {

        String sessionId = (dto.sessionId() != null)
                ? dto.sessionId()
                : UUID.randomUUID().toString();

        String userId = user.getId().toString();

        List<MessageDTO> history = chatHistoryService.getHistory(userId, sessionId);

        List<MessageDTO> messagesToSend = new ArrayList<>(history);
        messagesToSend.add(new MessageDTO(MessageRole.USER, dto.message()));

        long startTime = System.currentTimeMillis();

        List<String> context = aiServiceClient.search(dto.message());

        AiServiceClient.ChatCompletion completion = aiServiceClient.chat(messagesToSend, context);

        int latencyMs = (int) (System.currentTimeMillis() - startTime);

        List<MessageDTO> updatedHistory = new ArrayList<>(messagesToSend);
        updatedHistory.add(new MessageDTO(MessageRole.ASSISTANT, completion.answer()));
        chatHistoryService.saveHistory(userId, sessionId, updatedHistory);

        try {
            auditService.save(
                    user,
                    sessionId,
                    dto.message(),
                    completion.answer(),
                    completion.tokensUsed(),
                    latencyMs,
                    ipAddress
            );
        } catch (Exception e) {
            log.error("Failed to audit interaction. userId={} sessionId={}", userId, sessionId, e);
        }

        return new ChatResponseDTO(sessionId, completion.answer(), completion.tokensUsed());
    }
}

package br.com.api_core.modules.chat;

import br.com.api_core.domain.User;
import br.com.api_core.infra.security.SecurityUtils;
import br.com.api_core.modules.chat.dto.ChatRequestDTO;
import br.com.api_core.modules.chat.dto.ChatResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final SecurityUtils securityUtils;

    public ChatController(ChatService chatService,
                          SecurityUtils securityUtils) {
        this.chatService = chatService;
        this.securityUtils = securityUtils;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping
    public ResponseEntity<ChatResponseDTO> sendMessage(
            @Valid @RequestBody ChatRequestDTO dto,
            Authentication authentication,
            HttpServletRequest request) {

        User user = securityUtils.getAuthenticatedUser(authentication);
        String ipAddress = extractClientIp(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(chatService.sendMessage(dto, user, ipAddress));
    }
}

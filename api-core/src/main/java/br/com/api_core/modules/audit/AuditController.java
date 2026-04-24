package br.com.api_core.modules.audit;

import br.com.api_core.infra.security.SecurityUtils;
import br.com.api_core.modules.audit.dto.AuditLogResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;
    private final SecurityUtils securityUtils;

    public AuditController(AuditService auditService,
                           SecurityUtils securityUtils) {
        this.auditService = auditService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<List<AuditLogResponseDTO>> findByUser(
            Authentication authentication) {

        UUID userId = securityUtils.getAuthenticatedUserId(authentication);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(auditService.findByUser(userId));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<AuditLogResponseDTO>> findByUserIdAndSessionId(
            @PathVariable String sessionId,
            Authentication authentication) {

        UUID userId = securityUtils.getAuthenticatedUserId(authentication);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(auditService.findByUserIdAndSessionId(userId, sessionId));
    }
}

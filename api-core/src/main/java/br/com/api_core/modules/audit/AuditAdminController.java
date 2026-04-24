package br.com.api_core.modules.audit;

import br.com.api_core.modules.audit.dto.AuditLogResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit")
public class AuditAdminController {

    private final AuditService auditService;

    public AuditAdminController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<AuditLogResponseDTO>> findAllAuditLogs() {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(auditService.findAll());
    }
}

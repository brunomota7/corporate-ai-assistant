package br.com.api_core.modules.audit;

import br.com.api_core.domain.AuditLog;
import br.com.api_core.domain.User;
import br.com.api_core.domain.repository.AuditLogRepository;
import br.com.api_core.modules.audit.dto.AuditLogResponseDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    private AuditLogResponseDTO toResponseDTO(AuditLog auditLog) {

        return new AuditLogResponseDTO(
                auditLog.getId(),
                auditLog.getSessionId(),
                auditLog.getQuestion(),
                auditLog.getAnswer(),
                auditLog.getTokensUsed(),
                auditLog.getLatencyMs(),
                auditLog.getIpAddress(),
                auditLog.getCreatedAt()
        );
    }

    public void save(
            User user,
            String sessionId,
            String question,
            String answer,
            Integer tokenUsed,
            Integer latencyMs,
            String ipAddress
    ) {

        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setSessionId(sessionId);
        log.setQuestion(question);
        log.setAnswer(answer);
        log.setTokensUsed(tokenUsed);
        log.setLatencyMs(latencyMs);
        log.setIpAddress(ipAddress);

        auditLogRepository.save(log);
    }

    public List<AuditLogResponseDTO> findByUser(UUID userId) {

        return auditLogRepository.findByUserId(userId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<AuditLogResponseDTO> findByUserIdAndSessionId(UUID userId, String sessionId) {

        return auditLogRepository.findByUserIdAndSessionId(userId, sessionId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<AuditLogResponseDTO> findAll() {

        return auditLogRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }
 }

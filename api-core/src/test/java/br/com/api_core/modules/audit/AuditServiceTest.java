package br.com.api_core.modules.audit;

import br.com.api_core.domain.AuditLog;
import br.com.api_core.domain.User;
import br.com.api_core.domain.enums.Role;
import br.com.api_core.domain.repository.AuditLogRepository;
import br.com.api_core.modules.audit.dto.AuditLogResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static br.com.api_core.support.TestUtils.setField;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setName("Jhon");
        user.setEmail("jhon@teste.com");
        user.setRole(Role.ROLE_USER);
        setField(user, "id", userId);
    }

    @Test
    void save_shouldPersistAuditLogWithAllFields() {
        String sessionId = UUID.randomUUID().toString();

        auditService.save(
                user,
                sessionId,
                "Qual o estoque?",
                "O estoque atual é 10 unidades.",
                42,
                320,
                "192.168.0.1"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals(user, saved.getUser());
        assertEquals(sessionId, saved.getSessionId());
        assertEquals("Qual o estoque?", saved.getQuestion());
        assertEquals("O estoque atual é 10 unidades.", saved.getAnswer());
        assertEquals(42, saved.getTokensUsed());
        assertEquals(320, saved.getLatencyMs());
        assertEquals("192.168.0.1", saved.getIpAddress());
    }

    @Test
    void save_shouldPersist_whenOptionalFieldsAreNull() {
        // tokensUsed e latencyMs são nullable — falha no ai-service não impede auditoria
        auditService.save(user, "session-1", "Pergunta", "Resposta", null, null, null);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void findByUser_shouldReturnLogsForUser() {
        AuditLog log = buildAuditLog("Pergunta 1", "Resposta 1");

        when(auditLogRepository.findByUserId(userId)).thenReturn(List.of(log));

        List<AuditLogResponseDTO> result = auditService.findByUser(userId);

        assertEquals(1, result.size());
        assertEquals("Pergunta 1", result.get(0).question());
        assertEquals("Resposta 1", result.get(0).answer());
    }

    @Test
    void findByUser_shouldReturnEmptyList_whenNoLogsExist() {
        when(auditLogRepository.findByUserId(userId)).thenReturn(List.of());

        List<AuditLogResponseDTO> result = auditService.findByUser(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByUserIdAndSessionId_shouldReturnLogsForUserAndSession() {
        String sessionId = "session-abc";
        AuditLog log = buildAuditLog("Sessão pergunta", "Sessão resposta");
        setField(log, "sessionId", sessionId);

        when(auditLogRepository.findByUserIdAndSessionId(userId, sessionId))
                .thenReturn(List.of(log));

        List<AuditLogResponseDTO> result = auditService.findByUserIdAndSessionId(userId, sessionId);

        assertEquals(1, result.size());
        assertEquals("Sessão pergunta", result.get(0).question());
    }

    @Test
    void findAll_shouldReturnAllLogs() {
        AuditLog log1 = buildAuditLog("P1", "R1");
        AuditLog log2 = buildAuditLog("P2", "R2");

        when(auditLogRepository.findAll()).thenReturn(List.of(log1, log2));

        List<AuditLogResponseDTO> result = auditService.findAll();

        assertEquals(2, result.size());
    }

    private AuditLog buildAuditLog(String question, String answer) {
        AuditLog log = new AuditLog();
        setField(log, "id", UUID.randomUUID());
        log.setUser(user);
        log.setSessionId(UUID.randomUUID().toString());
        log.setQuestion(question);
        log.setAnswer(answer);
        log.setTokensUsed(10);
        log.setLatencyMs(200);
        log.setIpAddress("10.0.0.1");
        setField(log, "createdAt", LocalDateTime.now());
        return log;
    }
}
package br.com.api_core.domain.repository;

import br.com.api_core.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByUserId(UUID userId);
    List<AuditLog> findBySessionId(UUID sessionId);
    List<AuditLog> findByUserIdAndSessionId(UUID userId, UUID sessionId);
}

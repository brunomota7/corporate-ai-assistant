package br.com.notification_service.domain.repository;

import br.com.notification_service.domain.NotificationLog;
import br.com.notification_service.domain.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {
    List<NotificationLog> findByRecipient(String recipient);
    List<NotificationLog> findByStatus(NotificationStatus status);
    List<NotificationLog> findByEventType(String eventType);
}

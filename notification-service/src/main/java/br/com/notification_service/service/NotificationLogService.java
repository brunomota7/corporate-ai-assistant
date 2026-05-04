package br.com.notification_service.service;

import br.com.notification_service.domain.NotificationLog;
import br.com.notification_service.domain.enums.NotificationStatus;
import br.com.notification_service.domain.repository.NotificationLogRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class NotificationLogService {

    private final NotificationLogRepository notificationLogRepository;

    public NotificationLogService(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    @Transactional
    public NotificationLog createPending(String eventType,
                                         String recipient,
                                         String subject) {

        NotificationLog log = new NotificationLog();
        log.setEventType(eventType);
        log.setRecipient(recipient);
        log.setSubject(subject);

        return notificationLogRepository.save(log);
    }

    @Transactional
    public void markAsSent(NotificationLog notificationLog) {
        notificationLog.setStatus(NotificationStatus.SENT);
        notificationLog.setSentAt(LocalDateTime.now());
        notificationLogRepository.save(notificationLog);
    }

    @Transactional
    public void markAsFailed(NotificationLog notificationLog, String errorMessage) {
        notificationLog.setStatus(NotificationStatus.FAILED);
        notificationLog.setErrorMessage(errorMessage);
        notificationLogRepository.save(notificationLog);
    }
}

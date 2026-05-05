package br.com.notification_service.service;

import br.com.notification_service.domain.NotificationLog;
import br.com.notification_service.domain.enums.NotificationStatus;
import br.com.notification_service.domain.repository.NotificationLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationLogServiceTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @InjectMocks
    private NotificationLogService notificationLogService;

    private NotificationLog buildLog() {
        NotificationLog log = new NotificationLog();
        log.setEventType("order.confirmed");
        log.setRecipient("bruno@example.com");
        log.setSubject("Pedido confirmado");
        return log;
    }

    @Test
    void createPending_shouldPersistLogWithCorrectFields() {
        when(notificationLogRepository.save(any(NotificationLog.class)))
                .thenAnswer(ivn -> ivn.getArgument(0));

        NotificationLog result = notificationLogService.createPending(
                "user.registered",
                "jhon@example.com",
                "Bem-vindo ao Corporate AI Assistant"
        );

        ArgumentCaptor<NotificationLog> captor =
                ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());

        NotificationLog saved = captor.getValue();
        assertEquals("user.registered", saved.getEventType());
        assertEquals("jhon@example.com", saved.getRecipient());
        assertEquals("Bem-vindo ao Corporate AI Assistant", saved.getSubject());
    }

    @Test
    void markAsSent_shouldUpdateStatusAndSentAt() {
        NotificationLog log = buildLog();
        when(notificationLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationLogService.markAsSent(log);

        assertEquals(NotificationStatus.SENT, log.getStatus());
        assertNotNull(log.getSentAt());
        verify(notificationLogRepository).save(log);
    }

    @Test
    void markAsFailed_shouldUpdateStatusAndErrorMessage() {
        NotificationLog log = buildLog();
        when(notificationLogRepository.save(any())).thenAnswer(ivn -> ivn.getArgument(0));

        notificationLogService.markAsFailed(log, "SMTP timeout");

        assertEquals(NotificationStatus.FAILED, log.getStatus());
        assertEquals("SMTP timeout", log.getErrorMessage());
        assertNull(log.getSentAt());
        verify(notificationLogRepository).save(log);
    }
}
package br.com.notification_service.consumer;

import br.com.notification_service.config.RabbitMQConfig;
import br.com.notification_service.domain.NotificationLog;
import br.com.notification_service.dto.UserRegisteredEventDTO;
import br.com.notification_service.service.EmailService;
import br.com.notification_service.service.NotificationLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEventConsumerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationLogService notificationLogService;

    @InjectMocks
    private UserEventConsumer userEventConsumer;

    @Test
    void onUserRegistered_shouldCreateLogAndSendEmail_onSuccess() {
        UserRegisteredEventDTO event = new UserRegisteredEventDTO(
                "uuid-1", "Bruno", "bruno@example.com");

        NotificationLog log = new NotificationLog();
        when(notificationLogService.createPending(any(), any(), any())).thenReturn(log);

        userEventConsumer.onUserRegistered(event);

        verify(notificationLogService).createPending(
                eq(RabbitMQConfig.RK_USER_REGISTERED),
                eq("bruno@example.com"),
                any()
        );
        verify(emailService).sendWelcome(event);
        verify(notificationLogService).markAsSent(log);
        verify(notificationLogService, never()).markAsFailed(any(), any());
    }

    @Test
    void onUserRegistered_shouldMarkAsFailed_whenEmailServiceThrows() {
        UserRegisteredEventDTO event = new UserRegisteredEventDTO(
                "uuid-1", "Bruno", "bruno@example.com");

        NotificationLog log = new NotificationLog();
        when(notificationLogService.createPending(any(), any(), any())).thenReturn(log);
        doThrow(new RuntimeException("SMTP timeout"))
                .when(emailService).sendWelcome(event);

        assertThrows(RuntimeException.class,
                () -> userEventConsumer.onUserRegistered(event));

        verify(notificationLogService).markAsFailed(eq(log), eq("SMTP timeout"));
        verify(notificationLogService, never()).markAsSent(any());
    }

    @Test
    void onUserRegistered_shouldRethrowException_toTriggerRetry() {
        UserRegisteredEventDTO event = new UserRegisteredEventDTO(
                "uuid-1", "Bruno", "bruno@example.com");

        when(notificationLogService.createPending(any(), any(), any()))
                .thenReturn(new NotificationLog());
        doThrow(new RuntimeException("SMTP timeout"))
                .when(emailService).sendWelcome(event);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userEventConsumer.onUserRegistered(event));

        assertTrue(ex.getMessage().contains("boas-vindas"));
    }
}
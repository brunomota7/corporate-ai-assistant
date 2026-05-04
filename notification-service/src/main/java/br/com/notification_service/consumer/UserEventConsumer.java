package br.com.notification_service.consumer;

import br.com.notification_service.config.RabbitMQConfig;
import br.com.notification_service.dto.UserRegisteredEventDTO;
import br.com.notification_service.service.EmailService;
import br.com.notification_service.service.NotificationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserEventConsumer {

    private final EmailService emailService;
    private final NotificationLogService notificationLogService;

    public UserEventConsumer(EmailService emailService,
                             NotificationLogService notificationLogService) {
        this.emailService = emailService;
        this.notificationLogService = notificationLogService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_USER)
    public void onUserRegistered(UserRegisteredEventDTO event) {
        log.info("Evento recebido: user.registered — recipient={}", event.email());

        var logEntry = notificationLogService.createPending(
                RabbitMQConfig.RK_USER_REGISTERED,
                event.email(),
                "Bem-vindo ao Corporate AI Assistant"
        );

        try {
            emailService.sendWelcome(event);
            notificationLogService.markAsSent(logEntry);
            log.info("E-mail de boas-vindas enviado para {}", event.email());

        } catch (Exception e) {
            notificationLogService.markAsFailed(logEntry, e.getMessage());
            log.error("Falha ao enviar e-mail de boas-vindas para {}: {}",
                    event.email(), e.getMessage());

            throw new RuntimeException("Falha no envio do e-mail de boas-vindas", e);
        }
    }
}
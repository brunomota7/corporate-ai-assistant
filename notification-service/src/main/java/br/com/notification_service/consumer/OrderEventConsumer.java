package br.com.notification_service.consumer;

import br.com.notification_service.config.RabbitMQConfig;
import br.com.notification_service.dto.OrderCancelledEventDTO;
import br.com.notification_service.dto.OrderConfirmedEventDTO;
import br.com.notification_service.dto.OrderStatusChangedEventDTO;
import br.com.notification_service.service.EmailService;
import br.com.notification_service.service.NotificationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventConsumer {

    private final EmailService emailService;
    private final NotificationLogService notificationLogService;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(EmailService emailService,
                              NotificationLogService notificationLogService,
                              ObjectMapper objectMapper) {
        this.emailService = emailService;
        this.notificationLogService = notificationLogService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER)
    public void onOrderEvent(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        log.info("Evento recebido na order.queue — routingKey={}", routingKey);

        try {
            switch (routingKey) {
                case RabbitMQConfig.RK_ORDER_CONFIRMED      -> handleOrderConfirmed(message);
                case RabbitMQConfig.RK_ORDER_STATUS_CHANGED -> handleOrderStatusChanged(message);
                case RabbitMQConfig.RK_ORDER_CANCELLED      -> handleOrderCancelled(message);
                default -> log.warn("Routing key desconhecida ignorada: {}", routingKey);
            }
        } catch (Exception e) {
            log.error("Falha ao processar evento order — routingKey={}: {}", routingKey, e.getMessage());
            throw new RuntimeException("Falha no processamento do evento: " + routingKey, e);
        }
    }

    private void handleOrderConfirmed(Message message) throws Exception {
        OrderConfirmedEventDTO event = objectMapper.readValue(
                message.getBody(), OrderConfirmedEventDTO.class);

        log.info("Processando order.confirmed — orderId={}", event.orderId());

        var logEntry = notificationLogService.createPending(
                RabbitMQConfig.RK_ORDER_CONFIRMED,
                event.userEmail(),
                "Pedido #" + event.orderId().substring(0, 8).toUpperCase() + " confirmado"
        );

        try {
            emailService.sendOrderConfirmed(event);
            notificationLogService.markAsSent(logEntry);
            log.info("E-mail de confirmação enviado para {}", event.userEmail());
        } catch (Exception e) {
            notificationLogService.markAsFailed(logEntry, e.getMessage());
            throw e;
        }
    }

    private void handleOrderStatusChanged(Message message) throws Exception {
        OrderStatusChangedEventDTO event = objectMapper.readValue(
                message.getBody(), OrderStatusChangedEventDTO.class);

        log.info("Processando order.status.changed — orderId={} status={}→{}",
                event.orderId(), event.oldStatus(), event.newStatus());

        var logEntry = notificationLogService.createPending(
                RabbitMQConfig.RK_ORDER_STATUS_CHANGED,
                event.userEmail(),
                "Atualização do pedido #" + event.orderId().substring(0, 8).toUpperCase()
        );

        try {
            emailService.sendOrderStatusChanged(event);
            notificationLogService.markAsSent(logEntry);
            log.info("E-mail de atualização de status enviado para {}", event.userEmail());
        } catch (Exception e) {
            notificationLogService.markAsFailed(logEntry, e.getMessage());
            throw e;
        }
    }

    private void handleOrderCancelled(Message message) throws Exception {
        OrderCancelledEventDTO event = objectMapper.readValue(
                message.getBody(), OrderCancelledEventDTO.class);

        log.info("Processando order.cancelled — orderId={}", event.orderId());

        var logEntry = notificationLogService.createPending(
                RabbitMQConfig.RK_ORDER_CANCELLED,
                event.userEmail(),
                "Pedido #" + event.orderId().substring(0, 8).toUpperCase() + " cancelado"
        );

        try {
            emailService.sendOrderCancelled(event);
            notificationLogService.markAsSent(logEntry);
            log.info("E-mail de cancelamento enviado para {}", event.userEmail());
        } catch (Exception e) {
            notificationLogService.markAsFailed(logEntry, e.getMessage());
            throw e;
        }
    }
}
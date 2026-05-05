package br.com.notification_service.consumer;

import br.com.notification_service.config.RabbitMQConfig;
import br.com.notification_service.domain.NotificationLog;
import br.com.notification_service.dto.OrderConfirmedEventDTO;
import br.com.notification_service.service.EmailService;
import br.com.notification_service.service.NotificationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationLogService notificationLogService;

    @InjectMocks
    private OrderEventConsumer orderEventConsumer;

    // ObjectMapper real — testa a desserialização de verdade
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void onOrderEvent_shouldRouteToConfirmed_andSendEmail() throws Exception {
        OrderConfirmedEventDTO event = new OrderConfirmedEventDTO(
                "order-uuid-abc123", "user-1", "Bruno", "bruno@example.com",
                new BigDecimal("4500.00"), null,
                List.of(new OrderConfirmedEventDTO.OrderItemDTO(
                        "Notebook Dell", 1, new BigDecimal("4500.00")))
        );

        OrderEventConsumer consumer = new OrderEventConsumer(
                emailService, notificationLogService, objectMapper);

        byte[] body = objectMapper.writeValueAsBytes(event);
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey(RabbitMQConfig.RK_ORDER_CONFIRMED);
        Message message = new Message(body, props);

        NotificationLog log = new NotificationLog();
        when(notificationLogService.createPending(any(), any(), any())).thenReturn(log);

        consumer.onOrderEvent(message);

        verify(emailService).sendOrderConfirmed(any(OrderConfirmedEventDTO.class));
        verify(notificationLogService).markAsSent(log);
        verify(notificationLogService, never()).markAsFailed(any(), any());
    }

    @Test
    void onOrderEvent_shouldIgnoreUnknownRoutingKey() throws Exception {
        OrderEventConsumer consumer = new OrderEventConsumer(
                emailService, notificationLogService, objectMapper);

        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("unknown.event");
        Message message = new Message("{}".getBytes(), props);

        assertDoesNotThrow(() -> consumer.onOrderEvent(message));
        verifyNoInteractions(emailService);
        verifyNoInteractions(notificationLogService);
    }

    @Test
    void onOrderEvent_shouldRethrowException_whenEmailFails() throws Exception {
        OrderConfirmedEventDTO event = new OrderConfirmedEventDTO(
                "order-uuid-abc123", "user-1", "Bruno", "bruno@example.com",
                new BigDecimal("4500.00"), null, List.of()
        );

        OrderEventConsumer consumer = new OrderEventConsumer(
                emailService, notificationLogService, objectMapper);

        byte[] body = objectMapper.writeValueAsBytes(event);
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey(RabbitMQConfig.RK_ORDER_CONFIRMED);
        Message message = new Message(body, props);

        when(notificationLogService.createPending(any(), any(), any()))
                .thenReturn(new NotificationLog());
        doThrow(new RuntimeException("SMTP error"))
                .when(emailService).sendOrderConfirmed(any());

        assertThrows(RuntimeException.class, () -> consumer.onOrderEvent(message));
    }
}
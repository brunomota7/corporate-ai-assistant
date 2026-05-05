package br.com.api_core.infra.messaging;

import br.com.api_core.config.RabbitMQConfig;
import br.com.api_core.infra.messaging.dto.OrderCancelledEventDTO;
import br.com.api_core.infra.messaging.dto.OrderConfirmedEventDTO;
import br.com.api_core.infra.messaging.dto.OrderStatusChangedEventDTO;
import br.com.api_core.infra.messaging.dto.UserRegisteredEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    private void publish(String routingKey, Object event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    routingKey,
                    event
            );
            log.info("Event published - routingKey={}", routingKey);
        } catch (Exception e) {
            log.error("Failed to publish event - routingKey={}",
                    routingKey, e.getMessage());
        }
    }

    public void publishUserRegistered(UserRegisteredEventDTO event) {
        publish(RabbitMQConfig.RK_USER_REGISTERED, event);
    }

    public void publishOrderConfirmed(OrderConfirmedEventDTO event) {
        publish(RabbitMQConfig.RK_ORDER_CONFIRMED, event);
    }

    public void publishOrderStatusChanged(OrderStatusChangedEventDTO event) {
        publish(RabbitMQConfig.RK_ORDER_STATUS_CHANGED, event);
    }

    public void publishOrderCancelled(OrderCancelledEventDTO event) {
        publish(RabbitMQConfig.RK_ORDER_CANCELLED, event);
    }
}

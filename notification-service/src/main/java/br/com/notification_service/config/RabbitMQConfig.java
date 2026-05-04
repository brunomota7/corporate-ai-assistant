package br.com.notification_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "corporate.exchange";

    public static final String RK_USER_REGISTERED        = "user.registered";
    public static final String RK_ORDER_CONFIRMED        = "order.confirmed";
    public static final String RK_ORDER_STATUS_CHANGED   = "order.status.changed";
    public static final String RK_ORDER_CANCELLED        = "order.cancelled";

    public static final String QUEUE_USER    = "user.queue";
    public static final String QUEUE_ORDER   = "order.queue";

    public static final String QUEUE_USER_DLQ  = "user.queue.dlq";
    public static final String QUEUE_ORDER_DLQ = "order.queue.dlq";

    public static final String EXCHANGE_DLQ = "corporate.exchange.dlq";

    @Bean
    public DirectExchange corporateExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange corporateDlqExchange() {
        return new DirectExchange(EXCHANGE_DLQ, true, false);
    }

    @Bean
    public Queue userQueue() {
        return QueueBuilder.durable(QUEUE_USER)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLQ)
                .withArgument("x-dead-letter-routing-key", QUEUE_USER_DLQ)
                .build();
    }

    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(QUEUE_ORDER)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLQ)
                .withArgument("x-dead-letter-routing-key", QUEUE_ORDER_DLQ)
                .build();
    }

    @Bean
    public Queue userDlq() {
        return QueueBuilder.durable(QUEUE_USER_DLQ).build();
    }

    @Bean
    public Queue orderDlq() {
        return QueueBuilder.durable(QUEUE_ORDER_DLQ).build();
    }

    @Bean
    public Binding bindingUserRegistered() {
        return BindingBuilder
                .bind(userQueue())
                .to(corporateExchange())
                .with(RK_USER_REGISTERED);
    }

    @Bean
    public Binding bindingOrderConfirmed() {
        return BindingBuilder
                .bind(orderQueue())
                .to(corporateExchange())
                .with(RK_ORDER_CONFIRMED);
    }

    @Bean
    public Binding bindingOrderStatusChanged() {
        return BindingBuilder
                .bind(orderQueue())
                .to(corporateExchange())
                .with(RK_ORDER_STATUS_CHANGED);
    }

    @Bean
    public Binding bindingOrderCancelled() {
        return BindingBuilder
                .bind(orderQueue())
                .to(corporateExchange())
                .with(RK_ORDER_CANCELLED);
    }

    @Bean
    public Binding bindingUserDlq() {
        return BindingBuilder
                .bind(userDlq())
                .to(corporateDlqExchange())
                .with(QUEUE_USER_DLQ);
    }

    @Bean
    public Binding bindingOrderDlq() {
        return BindingBuilder
                .bind(orderDlq())
                .to(corporateDlqExchange())
                .with(QUEUE_ORDER_DLQ);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        return factory;
    }
}
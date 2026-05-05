package br.com.notification_service.service;

import br.com.notification_service.dto.OrderCancelledEventDTO;
import br.com.notification_service.dto.OrderConfirmedEventDTO;
import br.com.notification_service.dto.OrderStatusChangedEventDTO;
import br.com.notification_service.dto.UserRegisteredEventDTO;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@corporate.com");
    }

    @Test
    void sendWelcome_shouldProcessCorrectTemplateAndSend() {
        UserRegisteredEventDTO event = new UserRegisteredEventDTO(
                "uuid-1", "Jhon", "jhon@example.com");

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("welcome"), any(Context.class)))
                .thenReturn("<html>Bem-vindo Bruno</html>");

        emailService.sendWelcome(event);

        verify(templateEngine).process(eq("welcome"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendOrderConfirmed_shouldProcessCorrectTemplateAndSend() {
        OrderConfirmedEventDTO event = new OrderConfirmedEventDTO(
                "order-uuid-123", "user-1", "Bruno", "bruno@example.com",
                new BigDecimal("4500.00"), "Entrega urgente",
                List.of(new OrderConfirmedEventDTO.OrderItemDTO(
                        "Notebook Dell", 1, new BigDecimal("4500.00")))
        );

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("order-confirmed"), any(Context.class)))
                .thenReturn("<html>Pedido confirmado</html>");

        emailService.sendOrderConfirmed(event);

        verify(templateEngine).process(eq("order-confirmed"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendOrderStatusChanged_shouldProcessCorrectTemplateAndSend() {
        OrderStatusChangedEventDTO event = new OrderStatusChangedEventDTO(
                "order-uuid-456", "user-1", "Bruno", "bruno@example.com",
                "PENDING", "SHIPPED"
        );

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("order-status-changed"), any(Context.class)))
                .thenReturn("<html>Status atualizado</html>");

        emailService.sendOrderStatusChanged(event);

        verify(templateEngine).process(eq("order-status-changed"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendOrderCancelled_shouldProcessCorrectTemplateAndSend() {
        OrderCancelledEventDTO event = new OrderCancelledEventDTO(
                "order-uuid-789", "user-1", "Bruno", "bruno@example.com",
                new BigDecimal("4500.00")
        );

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("order-cancelled"), any(Context.class)))
                .thenReturn("<html>Pedido cancelado</html>");

        emailService.sendOrderCancelled(event);

        verify(templateEngine).process(eq("order-cancelled"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendWelcome_shouldThrowRuntimeException_whenMailSenderFails() {
        UserRegisteredEventDTO event = new UserRegisteredEventDTO(
                "uuid-1", "Bruno", "bruno@example.com");

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("welcome"), any(Context.class)))
                .thenReturn("<html>Bem-vindo</html>");
        doThrow(new RuntimeException("SMTP error"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThrows(RuntimeException.class, () -> emailService.sendWelcome(event));
    }
}
package br.com.notification_service.service;

import br.com.notification_service.dto.OrderCancelledEventDTO;
import br.com.notification_service.dto.OrderConfirmedEventDTO;
import br.com.notification_service.dto.OrderStatusChangedEventDTO;
import br.com.notification_service.dto.UserRegisteredEventDTO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender,
                        TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email to " + to, e);
        }
    }

    private String translateStatus(String status) {
        return switch (status) {
            case "PENDING"    -> "Pendente";
            case "PROCESSING" -> "Em processamento";
            case "SHIPPED"    -> "Enviado";
            case "DELIVERED"  -> "Entregue";
            case "CANCELLED"  -> "Cancelado";
            default           -> status;
        };
    }

    public void sendWelcome(UserRegisteredEventDTO event) {
        Context ctx = new Context();
        ctx.setVariable("name", event.name());
        ctx.setVariable("email", event.email());

        String html = templateEngine.process("welcome", ctx);

        send(event.email(),
             "Bem-vindo ao Corporate AI Assistant",
                html);
    }

    public void sendOrderConfirmed(OrderConfirmedEventDTO event) {
        Context ctx = new Context();
        ctx.setVariable("userName", event.userName());
        ctx.setVariable("orderId", event.orderId());
        ctx.setVariable("totalAmount", event.totalAmount());
        ctx.setVariable("items", event.items());
        ctx.setVariable("notes", event.notes());

        String html = templateEngine.process("order-confirmed", ctx);

        send(event.userEmail(),
             "Pedido #" + event.orderId().substring(0, 8).toUpperCase() + " confirmado",
                html);

    }

    public void sendOrderStatusChanged(OrderStatusChangedEventDTO event) {
        Context ctx = new Context();
        ctx.setVariable("userName", event.userName());
        ctx.setVariable("orderId", event.orderId().substring(0, 8).toUpperCase());
        ctx.setVariable("oldStatus", translateStatus(event.oldStatus()));
        ctx.setVariable("newStatus", translateStatus(event.newStatus()));

        String html = templateEngine.process("order-status-changed", ctx);

        send(event.userEmail(),
                "Atualização do pedido #" + event.orderId().substring(0, 8).toUpperCase(),
                html);
    }

    public void sendOrderCancelled(OrderCancelledEventDTO event) {
        Context ctx = new Context();
        ctx.setVariable("userName", event.userName());
        ctx.setVariable("orderId", event.orderId().substring(0, 8).toUpperCase());
        ctx.setVariable("totalAmount", event.totalAmount());

        String html = templateEngine.process("order-cancelled", ctx);

        send(event.userEmail(),
                "Pedido #" + event.orderId().substring(0, 8).toUpperCase() + " cancelado",
                html);
    }
}

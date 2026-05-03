package br.com.notification_service.domain;

import br.com.notification_service.domain.enums.NotificationStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_notification_log")
public class NotificationLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType;

    @Column(name = "recipient", length = 180, nullable = false)
    private String recipient;

    @Column(name = "subject", length = 255, nullable = false)
    private String subject;

    @Column(name = "status", length = 20, nullable = false)
    private NotificationStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @PrePersist
    private void prePersist() {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.status = NotificationStatus.PENDING;
    }

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}

    public String getEventType() {return eventType;}
    public void setEventType(String eventType) {this.eventType = eventType;}

    public String getRecipient() {return recipient;}
    public void setRecipient(String recipient) {this.recipient = recipient;}

    public String getSubject() {return subject;}
    public void setSubject(String subject) {this.subject = subject;}

    public NotificationStatus getStatus() {return status;}
    public void setStatus(NotificationStatus status) {this.status = status;}

    public String getErrorMessage() {return errorMessage;}
    public void setErrorMessage(String errorMessage) {this.errorMessage = errorMessage;}

    public LocalDateTime getCreatedAt() {return createdAt;}
    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}

    public LocalDateTime getSentAt() {return sentAt;}
    public void setSentAt(LocalDateTime sentAt) {this.sentAt = sentAt;}
}

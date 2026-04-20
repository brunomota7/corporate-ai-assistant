package br.com.api_core.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_audit_logs")
public class AuditLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_id", length = 100, nullable = false)
    private String sessionId;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;
    
    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;
    
    @Column(name = "tokens_used", nullable = true)
    private Integer tokensUsed;

    @Column(name = "latency_ms", nullable = true)
    private Integer latencyMs;
    
    @Column(name = "ip_address", length = 45, nullable = true)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}

    public User getUser() {return user;}
    public void setUser(User user) {this.user = user;}

    public String getSessionId() {return sessionId;}
    public void setSessionId(String sessionId) {this.sessionId = sessionId;}

    public String getQuestion() {return question;}
    public void setQuestion(String question) {this.question = question;}

    public String getAnswer() {return answer;}
    public void setAnswer(String answer) {this.answer = answer;}

    public Integer getTokensUsed() {return tokensUsed;}
    public void setTokensUsed(Integer tokensUsed) {this.tokensUsed = tokensUsed;}

    public Integer getLatencyMs() {return latencyMs;}
    public void setLatencyMs(Integer latencyMs) {this.latencyMs = latencyMs;}

    public String getIpAddress() {return ipAddress;}
    public void setIpAddress(String ipAddress) {this.ipAddress = ipAddress;}

    public LocalDateTime getCreatedAt() {return createdAt;}
    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}
}

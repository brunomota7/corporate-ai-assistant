package br.com.api_core.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tb_documents")
public class Document {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "source_type", length = 50, nullable = false)
    private String sourceType;

    @Column(name = "source_id", nullable = true)
    private UUID sourceId;

    @Column(name = "embedding", nullable = true, columnDefinition = "vector(1536)")
    private float[] embedding;

    @Column(name = "metadata", nullable = true, columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}

    public String getTitle() {return title;}
    public void setTitle(String title) {this.title = title;}

    public String getContent() {return content;}
    public void setContent(String content) {this.content = content;}

    public String getSourceType() {return sourceType;}
    public void setSourceType(String sourceType) {this.sourceType = sourceType;}

    public UUID getSourceId() {return sourceId;}
    public void setSourceId(UUID sourceId) {this.sourceId = sourceId;}

    public float[] getEmbedding() {return embedding;}
    public void setEmbedding(float[] embedding) {this.embedding = embedding;}

    public Map<String, Object> getMetadata() {return metadata;}
    public void setMetadata(Map<String, Object> metadata) {this.metadata = metadata;}

    public LocalDateTime getCreatedAt() {return createdAt;}
    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}
}

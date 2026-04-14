package entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private LocalDateTime createdAt;

    public Entity() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
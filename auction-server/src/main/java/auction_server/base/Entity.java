package auction_server.base;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public abstract class Entity implements Serializable {
    protected String id;
    protected LocalDateTime createdAt;

    public Entity() {
        // Tự động tạo UUID nếu không có ID nào được cung cấp
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    public Entity(String id) {
        this.id = (id != null) ? id : UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

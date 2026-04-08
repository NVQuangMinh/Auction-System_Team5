package auction_system.entity;
import java.io.Serializable;
import java.time.LocalDateTime;

// implements Serializable để các class con có thể gửi qua socket
public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private LocalDateTime createdAt;

    public Entity() {
        this.createdAt = LocalDateTime.now();
    }

    // Thêm getter và setter
}
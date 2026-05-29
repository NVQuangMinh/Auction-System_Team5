package auction_shared.dto;

import java.io.Serializable;

public class UserDTO implements Serializable {
    private String id;
    private String username;
    private String role;

    public UserDTO(String id, String username, String role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return this.role;
    }
}

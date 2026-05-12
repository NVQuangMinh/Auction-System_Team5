package auction_shared.dto;

import java.io.Serializable;

public class UserDTO implements Serializable {
    private String id;
    private String username;
    private boolean isAdmin;

    public UserDTO(String id, String username) {
        this.id = id;
        this.username = username;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}

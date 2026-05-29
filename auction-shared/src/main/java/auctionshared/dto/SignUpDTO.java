package auctionshared.dto;

import java.io.Serializable;

public class SignUpDTO implements Serializable {
    private String id;
    private String username;
    private String password;

    public SignUpDTO(String id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getId() {
        return id;
    }
}

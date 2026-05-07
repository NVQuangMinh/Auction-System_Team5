package auction_server.service;

import auction_server.dao.UserDAO;
import auction_server.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    public boolean register(User user) {
        // TODO: Hash the password before saving.
        // For now, assuming user.getPasswordHash() already contains a hashed password or plain text for demonstration.
        if (userDAO.findByUsername(user.getUsername()) != null) {
            log.warn("Registration failed: Username '{}' already exists.", user.getUsername());
            return false;
        }
        userDAO.save(user);
        log.info("User '{}' registered successfully.", user.getUsername());
        return true;
    }

    public User login(String username, String password) {
        User user = userDAO.findByUsername(username);
        if (user != null) {
            // TODO: Implement proper password verification using a hashing algorithm.
            // For demonstration, we're comparing plain text or pre-hashed value.
            if (user.login(password)) { // user.login() now checks passwordHash
                log.info("User '{}' logged in successfully.", username);
                return user;
            }
        }
        log.warn("Login failed for username '{}'. Invalid credentials.", username);
        return null;
    }
}

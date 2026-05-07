package auction_server.service;

import auction_server.dao.interfaces.UserDAO;
import auction_server.entities.User;

public class UserService {
    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void register(String username, String password) throws Exception {
        if (userDAO.findUserByUsername(username) != null) {
            throw new Exception("Username already exists.");
        }
        // TODO: Implement password hashing here
        String hashedPassword = password; // Placeholder
        User newUser = new User(null, username, hashedPassword);
        userDAO.save(newUser);
    }

    public User login(String username, String password) throws Exception {
        User user = userDAO.findUserByUsername(username);
        // TODO: Implement password hashing comparison here
        if (user != null && user.getPasswordHash().equals(password)) {
            return user;
        }
        throw new Exception("Invalid username or password.");
    }
}

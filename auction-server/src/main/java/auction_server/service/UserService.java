package auction_server.service;

import auction_server.dao.UserDAO;
import auction_server.entities.User;

public class UserService {
    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    public boolean register(User user) {
        // Kiểm tra username đã tồn tại chưa
        if (userDAO.getUserByUsername(user.getUsername()) != null) {
            return false;
        }
        return userDAO.insertUser(user);
    }

    public User login(String username, String password) {
        User user = userDAO.getUserByUsername(username);
        if (user != null && user.getPassword().equals(password) && !user.getUserStatus().equals("BANNED")) {
            return user;
        }
        return null;
    }
}

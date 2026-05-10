package auction_server.service;

import auction_server.dao.UserDAO;
import auction_server.entities.User;

public class UserService {

    public boolean register(User user) {
        // Kiểm tra username đã tồn tại chưa
        if (UserDAO.getUserByUsername(user.getUsername()) != null) {
            return false;
        }
        return UserDAO.insertUser(user);
    }

    public User login(String username, String password) {
        User user = UserDAO.getUserByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }
}

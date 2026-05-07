package auction_server.dao.interfaces;

import auction_server.entities.User;

public interface UserDAO {
    User findByUsername(String username);
    void save(User user);
}
package auctionserver.service;

import auctionserver.dao.DAOProvider;
import auctionserver.dao.UserDAO;
import auctionserver.entities.User;
import auctionserver.exception.DatabaseException;
import auctionserver.exception.DuplicateUsernameException;
import auctionserver.exception.UserBannedException;
import auctionserver.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserDAO userDAO;

    public UserService(DAOProvider daoProvider) {
        this.userDAO = daoProvider.userDAO();
    }

    public void register(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        }

        try {
            User existingUser = userDAO.getUserByUsername(user.getUsername());
            if (existingUser != null) {
                throw new DuplicateUsernameException("Tên đăng nhập đã tồn tại: " + user.getUsername());
            }
            userDAO.insertUser(user);
            log.info("Người dùng đã đăng ký thành công: {}", user.getUsername());
        } catch (DatabaseException e) {
            log.error("Lỗi cơ sở dữ liệu khi người dùng đăng kí: {}", user.getUsername(), e);
            throw e;
        } catch (Exception e) {
            log.error("Lỗi cơ sở dữ liệu khi người dùng đăng kí: {}", user.getUsername(), e);
            throw new DatabaseException("Lỗi cơ sở dữ liệu khi người dùng đăng kí", e);
        }
    }

    public User login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        }

        try {
            User user = userDAO.getUserByUsername(username);
            if (user == null) {
                throw new UserNotFoundException("Người dùng không tồn tại: " + username);
            }
            if ("BANNED".equals(user.getUserStatus())) {
                throw new UserBannedException("Người dùng đã bị khoá: " + username);
            }
            if (!user.getPassword().equals(password)) {
                throw new UserNotFoundException("Thông tin xác thực của người dùng không hợp lệ: " + username);
            }
            log.info("Người dùng đã đăng nhập thành công: {}", username);
            return user;
        } catch (DatabaseException e) {
            log.error("Lỗi cơ sở dữ liệu khi người dùng đăng nhập: {}", username, e);
            throw e;
        } catch (UserBannedException | UserNotFoundException e) {
            log.error("Lỗi cơ sở dữ liệu khi người dùng đăng nhập: {}", username, e);
            throw e;
        }
    }
}

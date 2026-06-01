package auctionserver.dao;

import auctionserver.entities.User;
import auctionserver.exception.DatabaseException;
import auctionshared.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private static final Logger log = LoggerFactory.getLogger(UserDAO.class);

    public void insertUser(User user) {
        String sql = "INSERT INTO users (id, username, password, role, user_status) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getRole());
            pstmt.setString(5, user.getUserStatus());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Không thể thêm mới người dùng: " + user.getUsername());
            }
            log.info("Thêm mới người dùng thành công: {}", user.getUsername());
        } catch (SQLException e) {
            log.error("Lỗi cơ sở dữ liệu khi thêm mới người dùng: {}", user.getUsername(), e);
            throw new DatabaseException("Không thể thêm mới người dùng: " + user.getUsername(), e);
        }
    }

    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getString("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("role"),
                            rs.getString("user_status")
                    );
                }
            }
        } catch (SQLException e) {
            log.error("Lỗi cơ sở dữ liệu khi truy vẫn thông tin người dùng theo tên đăng nhập: {}", username, e);
            throw new DatabaseException("Không thể truy vẫn thông tin người dùng theo tên đăng nhập: " + username, e);
        }
        return null;
    }

    public List<User> getAllUsers() {
        String sql = "SELECT * FROM users WHERE role = 'USER' AND user_status = 'AVAILABLE'";
        List<User> users = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                User user = new User(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getString("user_status")
                );
                users.add(user);
            }
        } catch (SQLException e) {
            log.error("Lỗi cơ sở dữ liệu khi truy vấn tất cả người dùng.", e);
            throw new DatabaseException("Không thể truy vấn tất cả người dùng.", e);
        }
        return users;
    }

    public void userBan(UserDTO user) {
        String sql = "UPDATE users SET user_status = 'BANNED' WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Không thể khoá tài khoản người dùng: " + user.getUsername());
            }
            log.info("Đã khoá tài khoản người dùng: {}", user.getUsername());
        } catch (SQLException e) {
            log.error("Lỗi cơ sở dữ liệu khi khóa tài khoản người dùng: {}", user.getUsername(), e);
            throw new DatabaseException("Không thể khoá tài khoản người dùng: " + user.getUsername(), e);
        }
    }
}

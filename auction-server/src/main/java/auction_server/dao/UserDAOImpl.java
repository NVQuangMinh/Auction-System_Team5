package auction_server.dao;

import auction_server.behaviors.AdminProfile;
import auction_server.behaviors.BidderProfile;
import auction_server.behaviors.SellerProfile;
import auction_server.dao.interfaces.UserDAO;
import auction_server.entities.User;

import javax.sql.DataSource;
import java.sql.*;

public class UserDAOImpl implements UserDAO {

    private final DataSource dataSource;

    public UserDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public User findUserByUsername(String username) {
        // TODO: Hoàn thiện câu lệnh SQL cho phù hợp với schema của bạn
        String userSQL = "SELECT id, username, password_hash, created_at FROM users WHERE username = ?";
        String rolesSQL = "SELECT role_name FROM user_roles WHERE user_id = ?";
        User user = null;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement userPstmt = conn.prepareStatement(userSQL)) {

            userPstmt.setString(1, username);
            try (ResultSet rs = userPstmt.executeQuery()) {
                if (rs.next()) {
                    long userId = rs.getLong("id");
                    user = new User(
                            userId,
                            rs.getString("username"),
                            rs.getString("password_hash")
                    );
                    // user.createdAt có thể được set nếu cần

                    // Sau khi tìm thấy user, truy vấn vai trò của họ
                    try (PreparedStatement rolesPstmt = conn.prepareStatement(rolesSQL)) {
                        rolesPstmt.setLong(1, userId);
                        try (ResultSet rolesRs = rolesPstmt.executeQuery()) {
                            while (rolesRs.next()) {
                                String roleName = rolesRs.getString("role_name");
                                switch (roleName.toUpperCase()) {
                                    case "BIDDER":
                                        user.setBidderProfile(new BidderProfile());
                                        break;
                                    case "SELLER":
                                        user.setSellerProfile(new SellerProfile());
                                        break;
                                    case "ADMIN":
                                        user.setAdminProfile(new AdminProfile());
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            // Trong ứng dụng thực tế, nên sử dụng một exception tùy chỉnh
            throw new RuntimeException("Database error finding user by username", e);
        }
        return user;
    }

    @Override
    public void save(User user) {
        // TODO: Hoàn thiện câu lệnh SQL
        String sql = "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setTimestamp(3, Timestamp.valueOf(user.getCreatedAt()));

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getLong(1)); // Cập nhật ID cho đối tượng User
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error saving user", e);
        }
    }
}

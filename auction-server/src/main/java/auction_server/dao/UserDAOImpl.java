package auction_server.dao;

import auction_server.dao.interfaces.UserDAO;
import auction_server.entities.User;
import auction_server.behaviors.AdminProfile;
import auction_server.behaviors.BidderProfile;
import auction_server.behaviors.SellerProfile;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserDAOImpl implements UserDAO {

    public UserDAOImpl() {} // Constructor rỗng

    @Override
    public User findByUsername(String username) {
        String sqlUser = "SELECT * FROM users WHERE username = ?";
        String sqlRoles = "SELECT role_name FROM user_roles WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmtUser = conn.prepareStatement(sqlUser)) {

            pstmtUser.setString(1, username);
            try (ResultSet rsUser = pstmtUser.executeQuery()) {
                if (rsUser.next()) {
                    User user = new User(
                            rsUser.getString("id"),
                            rsUser.getString("username"),
                            rsUser.getString("password_hash")
                    );

                    // Lấy các vai trò (Roles)
                    try (PreparedStatement pstmtRoles = conn.prepareStatement(sqlRoles)) {
                        pstmtRoles.setString(1, user.getId());
                        try (ResultSet rsRoles = pstmtRoles.executeQuery()) {
                            while (rsRoles.next()) {
                                String role = rsRoles.getString("role_name");
                                switch (role) {
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
                    return user;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB khi tìm User theo username", e);
        }
        return null;
    }

    @Override
    public void save(User user) {
        String sqlUser = "INSERT INTO users (id, username, password_hash, created_at) VALUES (?, ?, ?, ?)";
        String sqlRole = "INSERT INTO user_roles (user_id, role_name) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Bắt đầu Transaction để đảm bảo tính toàn vẹn

            try (PreparedStatement pstmtUser = conn.prepareStatement(sqlUser)) {
                if (user.getId() == null) user.setId(UUID.randomUUID().toString());

                pstmtUser.setString(1, user.getId());
                pstmtUser.setString(2, user.getUsername());
                pstmtUser.setString(3, user.getPasswordHash());
                pstmtUser.setTimestamp(4, Timestamp.valueOf(user.getCreatedAt()));
                pstmtUser.executeUpdate();
            }

            // Lưu các Profile (Roles) vào bảng phụ
            try (PreparedStatement pstmtRole = conn.prepareStatement(sqlRole)) {
                List<String> roles = new ArrayList<>();
                if (user.hasRole(BidderProfile.class)) roles.add("BIDDER");
                if (user.hasRole(SellerProfile.class)) roles.add("SELLER");
                if (user.hasRole(AdminProfile.class)) roles.add("ADMIN");

                for (String role : roles) {
                    pstmtRole.setString(1, user.getId());
                    pstmtRole.setString(2, role);
                    pstmtRole.addBatch();
                }
                pstmtRole.executeBatch();
            }

            conn.commit(); // Hoàn tất Transaction
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB khi lưu User", e);
        }
    }
}
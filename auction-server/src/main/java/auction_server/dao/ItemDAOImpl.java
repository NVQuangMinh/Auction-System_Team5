package auction_server.dao;

import auction_server.dao.interfaces.ItemDAO;
import auction_server.entities.Item;
import auction_server.entities.User;
import auction_server.entities.items.Art;
import auction_server.entities.items.Electronics;
import auction_server.entities.items.Vehicle;

import javax.sql.DataSource;
import java.sql.*;

public class ItemDAOImpl implements ItemDAO {
    private final DataSource dataSource;

    public ItemDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Item findById(String id) {
        // Dùng JOIN để lấy luôn thông tin User sở hữu
        String sql = "SELECT i.*, u.username, u.password_hash " +
                "FROM items i JOIN users u ON i.owner_id = u.id " +
                "WHERE i.id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToItem(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error finding item by id", e);
        }
        return null;
    }

    @Override
    public Item findById(long id) {
        return null;
    }

    @Override
    public void save(Item item) {
        String sql = "INSERT INTO items (id, name, description, owner_id, type, created_at, artist_name, brand) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getId()); // UUID String
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription());
            pstmt.setString(4, item.getOwner().getId());
            pstmt.setString(5, item.getClass().getSimpleName().toLowerCase()); // art, electronics, vehicle
            pstmt.setTimestamp(6, Timestamp.valueOf(item.getCreatedAt()));

            if (item instanceof Art) {
                pstmt.setString(7, ((Art) item).getArtistName());
                pstmt.setNull(8, Types.VARCHAR);
            } else if (item instanceof Electronics) {
                pstmt.setNull(7, Types.VARCHAR);
                pstmt.setString(8, ((Electronics) item).getBrand());
            } else if (item instanceof Vehicle) {
                pstmt.setNull(7, Types.VARCHAR);
                pstmt.setString(8, ((Vehicle) item).getBrand());
            }

            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error saving item", e);
        }
    }

    private Item mapRowToItem(ResultSet rs) throws SQLException {
        // Khởi tạo đối tượng User thực tế từ thông tin JOIN
        User owner = new User(
                rs.getString("owner_id"),
                rs.getString("username"),
                rs.getString("password_hash")
        );

        String type = rs.getString("type");
        String itemId = rs.getString("id");
        String name = rs.getString("name");
        String description = rs.getString("description");

        switch (type.toLowerCase()) {
            case "art":
                return new Art(itemId, name, description, owner, rs.getString("artist_name"));
            case "electronics":
                return new Electronics(itemId, name, description, owner, rs.getString("brand"));
            case "vehicle":
                return new Vehicle(itemId, name, description, owner, rs.getString("brand"));
            default:
                throw new IllegalArgumentException("Unknown item type in DB: " + type);
        }
    }
}
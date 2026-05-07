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

    @Override
    public Item findById(long id) {
        // TODO: Hoàn thiện câu lệnh SQL. Cần JOIN với bảng User để lấy thông tin owner
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Tái tạo lại đúng đối tượng Item dựa vào cột 'type'
                    return mapRowToItem(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error finding item by id", e);
        }
        return null;
    }

    @Override
    public void save(Item item) {
        // TODO: Hoàn thiện câu lệnh SQL. Cần các cột đặc tả như artist_name, brand...
        String sql = "INSERT INTO items (name, description, owner_id, type, created_at, artist_name, brand) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getDescription());
            pstmt.setLong(3, item.getOwner().getId());
            pstmt.setTimestamp(5, Timestamp.valueOf(item.getCreatedAt()));

            // Xử lý các thuộc tính riêng của từng loại Item
            if (item instanceof Art) {
                pstmt.setString(4, "art");
                pstmt.setString(6, ((Art) item).getArtistName());
                pstmt.setNull(7, Types.VARCHAR);
            } else if (item instanceof Electronics) {
                pstmt.setString(4, "electronics");
                pstmt.setNull(6, Types.VARCHAR);
                pstmt.setString(7, ((Electronics) item).getBrand());
            } else if (item instanceof Vehicle) {
                pstmt.setString(4, "vehicle");
                pstmt.setNull(6, Types.VARCHAR);
                pstmt.setString(7, ((Vehicle) item).getBrand());
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        item.setId(generatedKeys.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error saving item", e);
        }
    }

    private Item mapRowToItem(ResultSet rs) throws SQLException {
        long itemId = rs.getLong("id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        String itemType = rs.getString("type");

        // TODO: Cần fetch đầy đủ đối tượng User từ DB, đây là ví dụ đơn giản
        long ownerId = rs.getLong("owner_id");
        User owner = new User(ownerId, "temp_owner", ""); // Tạm thời

        switch (itemType.toLowerCase()) {
            case "art":
                return new Art(itemId, name, description, owner, rs.getString("artist_name"));
            case "electronics":
                return new Electronics(itemId, name, description, owner, rs.getString("brand"));
            case "vehicle":
                return new Vehicle(itemId, name, description, owner, rs.getString("brand"));
            default:
                throw new IllegalArgumentException("Unknown item type in database: " + itemType);
        }
    }
}

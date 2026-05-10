package auction_server.dao;

import auction_server.entities.Item;
import auction_server.entities.User;
import auction_server.entities.items.Arts;
import auction_server.entities.items.Electronics;
import auction_server.entities.items.Vehicles;
import auction_server.interfaces.InterfaceDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ItemDAO implements InterfaceDAO<Item> {

    // Dùng để phân rã các item thành các instance của các loại item cụ thể để load vào DB
    private String resolveItemType(Item item) {
        if (item instanceof Arts) return "ARTS";
        if (item instanceof Electronics) return "ELECTRONICS";
        if (item instanceof Vehicles) return "VEHICLES";
        throw new IllegalArgumentException("Unknown item type: " + item.getClass().getSimpleName());
    }
    //Ngược lại với phân rã, dùng để dịch ngược
    // một dòng trong ResultSet thành một instance của một loại item cụ thể...
    private Item mapRow(ResultSet rs, User owner) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("item_name");
        String description = rs.getString("description");
        String type = rs.getString("item_type");
        return switch (type) {
            case "ARTS" -> new Arts(id, name, description, owner);
            case "ELECTRONICS" -> new Electronics(id, name, description, owner);
            case "VEHICLES" -> new Vehicles(id, name, description, owner);
            default -> throw new IllegalArgumentException("Unknown item_type: " + type);
        };
    }

    @Override
    public int insert(Item item) {
        String sql = "INSERT INTO items (id, item_type, item_name, description, owner_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, item.getId());
            pstmt.setString(2, resolveItemType(item));
            pstmt.setString(3, item.getName());
            pstmt.setString(4, item.getDescription());
            pstmt.setString(5, item.getOwner().getId());
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    // Lấy toàn bộ danh sách items từ DB để load vào AutionManager... yeah maybe stuff like that
    public ArrayList<Item> selectAll() {
        // Join table item và table users bằng owner_id
        String sql = "SELECT i.*, u.id as u_id, u.username, u.password FROM items i JOIN users u ON i.owner_id = u.id";

        ArrayList<Item> items = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                User owner = new User(rs.getString("u_id"), rs.getString("username"), rs.getString("password"));
                items.add(mapRow(rs, owner));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    @Override
    public int delete(Item item) {
        return 0;
    }

    @Override
    public int update(Item item) {
        return 0;
    }

    @Override
    public Item selectById(Item item) {
        return null;
    }

    @Override
    public ArrayList<Item> selectByCondition(String condition) {
        return null;
    }
}

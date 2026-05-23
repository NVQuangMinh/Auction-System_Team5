package auction_server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import auction_server.entities.Item;
import auction_server.entities.User;
import auction_server.entities.items.Arts;
import auction_server.entities.items.Electronics;
import auction_server.entities.items.Vehicles;
import auction_server.exception.DatabaseException;
import auction_server.factory.ItemFactory;
import auction_server.interfaces.ReadableDAO;
import auction_server.interfaces.WritableDAO;
import auction_shared.dto.ItemType;

@SuppressWarnings("rawtypes")
public class ItemDAO implements WritableDAO<Item>, ReadableDAO<Item> {
    private static final Logger log = LoggerFactory.getLogger(ItemDAO.class);

    private Item mapRow(ResultSet rs, User owner) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("item_name");
        String description = rs.getString("description");
        ItemType type = ItemType.fromDbValue(rs.getString("item_type"));
        return ItemFactory.of(type).create(id, name, description, owner,
                rs.getString(type.attributeColumn()));
    }

    @Override
    public int insert(Item item) {
        String sql = "INSERT INTO items (id, item_type, item_name, description, owner_id, artist_name, model, brand) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, item.getId());
            pstmt.setString(2, item.getType().toDbValue());
            pstmt.setString(3, item.getName());
            pstmt.setString(4, item.getDescription());
            pstmt.setString(5, item.getOwner().getId());
            pstmt.setString(6, item instanceof Arts ? (String) item.getTypeSpecificAttribute() : null);
            pstmt.setString(7, item instanceof Electronics ? (String) item.getTypeSpecificAttribute() : null);
            pstmt.setString(8, item instanceof Vehicles ? (String) item.getTypeSpecificAttribute() : null);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Database error while inserting item: {}", item.getId(), e);
            throw new DatabaseException("Failed to insert item: " + item.getId(), e);
        }
    }

    public int insert(Item item, Connection conn) throws SQLException {
        String sql = "INSERT INTO items (id, item_type, item_name, description, owner_id, artist_name, model, brand) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, item.getId());
            pstmt.setString(2, item.getType().toDbValue());
            pstmt.setString(3, item.getName());
            pstmt.setString(4, item.getDescription());
            pstmt.setString(5, item.getOwner().getId());
            pstmt.setString(6, item instanceof Arts ? (String) item.getTypeSpecificAttribute() : null);
            pstmt.setString(7, item instanceof Electronics ? (String) item.getTypeSpecificAttribute() : null);
            pstmt.setString(8, item instanceof Vehicles ? (String) item.getTypeSpecificAttribute() : null);
            return pstmt.executeUpdate();
        }
    }

    @Override
    // Lấy toàn bộ danh sách items từ DB để load vào AutionManager... yeah maybe
    // stuff like that
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
            log.error("Database error while selecting all items", e);
            throw new DatabaseException("Failed to select all items", e);
        }
        return items;
    }

    @Override
    public Item selectById(Item item) {
        return selectById(item.getId());
    }

    public Item selectById(String id) {
        String sql = "SELECT i.*, u.id as u_id, u.username, u.password " +
                     "FROM items i JOIN users u ON i.owner_id = u.id WHERE i.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User owner = new User(rs.getString("u_id"), rs.getString("username"), rs.getString("password"));
                    return mapRow(rs, owner);
                }
            }
        } catch (SQLException e) {
            log.error("Database error while selecting item by id: {}", id, e);
            throw new DatabaseException("Failed to select item: " + id, e);
        }
        return null;
    }

    @Override
    public ArrayList<Item> selectByCondition(String condition) {
        String sql = "SELECT i.*, u.id as u_id, u.username, u.password " +
                     "FROM items i JOIN users u ON i.owner_id = u.id WHERE " + condition;
        ArrayList<Item> items = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                User owner = new User(rs.getString("u_id"), rs.getString("username"), rs.getString("password"));
                items.add(mapRow(rs, owner));
            }
        } catch (SQLException e) {
            log.error("Database error while selecting items by condition: {}", condition, e);
            throw new DatabaseException("Failed to select items: " + condition, e);
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
}

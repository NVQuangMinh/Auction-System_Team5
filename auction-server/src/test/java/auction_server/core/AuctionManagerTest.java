package auction_server.core;

import auction_server.entities.Auction;
import auction_server.entities.User;
import auction_server.entities.items.Arts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho AuctionManager — Singleton quản lý các phòng đấu giá đang ACTIVE trong RAM.
 *
 * Trong hệ thống, AuctionManager đảm nhận:
 *   - Lưu trữ các Auction ACTIVE (Map<itemId, Auction> trong ConcurrentHashMap)
 *   - Tra cứu phòng khi có bid mới hoặc khi Scheduler check hết hạn
 *   - Broadcast NetworkMessage đến tất cả client đang kết nối
 *
 */
@DisplayName("AuctionManager Tests")
class AuctionManagerTest {

    private AuctionManager manager;

    @BeforeEach
    void setUp() {
        // Reset Singleton về null để mỗi test có instance sạch
        AuctionManager.resetForTest();
        manager = AuctionManager.getInstance();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Tạo một Auction mẫu với itemId cho trước */
    private Auction createAuction(String itemId) {
        User seller = new User("s-" + itemId, "seller_" + itemId, "pass", "USER", "AVAILABLE");
        Arts item = new Arts(itemId, "Item " + itemId, "desc", seller, "Artist");
        return new Auction(
                item, 100.0, 1000.0, 50.0,
                LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now().plusHours(2),
                false
        );
    }

    // ════════════════════════════════════════════════════════════════════════
    // Singleton
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getInstance - Luôn trả về cùng một instance (Singleton)")
    void getInstance_returnsSameInstance() {
        AuctionManager a = AuctionManager.getInstance();
        AuctionManager b = AuctionManager.getInstance();
        assertSame(a, b, "getInstance() phải trả về đúng cùng một object");
    }

    // ════════════════════════════════════════════════════════════════════════
    // addRoom / getRoom
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("addRoom + getRoom - Thêm auction rồi tra cứu → trả về đúng auction")
    void addRoom_andGetRoom_returnsCorrectAuction() {
        Auction auction = createAuction("item-001");
        manager.addRoom(auction);

        Auction found = manager.getRoom("item-001");

        assertSame(auction, found, "getRoom phải trả về đúng auction vừa add");
    }

    @Test
    @DisplayName("getRoom - itemId không tồn tại → trả về null")
    void getRoom_nonExistent_returnsNull() {
        assertNull(manager.getRoom("non-existent-id"),
                "Phải trả null khi không có room với itemId này");
    }

    // ════════════════════════════════════════════════════════════════════════
    // removeRoom
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("removeRoom - Sau khi remove, getRoom trả về null")
    void removeRoom_existingAuction_removesIt() {
        Auction auction = createAuction("item-002");
        manager.addRoom(auction);

        manager.removeRoom(auction);

        assertNull(manager.getRoom("item-002"),
                "Room phải bị xóa khỏi manager sau khi removeRoom");
    }

    @Test
    @DisplayName("removeRoom - Remove auction không tồn tại → không crash")
    void removeRoom_nonExistent_noOp() {
        Auction auction = createAuction("item-ghost");
        // Không add trước, remove thẳng → không crash
        assertDoesNotThrow(() -> manager.removeRoom(auction));
    }

    // ════════════════════════════════════════════════════════════════════════
    // getAllRooms
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getAllRooms - Add 2 auction → getAllRooms trả về list 2 phần tử")
    void getAllRooms_returnsAllAddedAuctions() {
        manager.addRoom(createAuction("item-A"));
        manager.addRoom(createAuction("item-B"));

        List<Auction> rooms = manager.getAllRooms();

        assertEquals(2, rooms.size(), "getAllRooms phải trả về đúng số room đang active");
    }

    @Test
    @DisplayName("getAllRooms - Trả về defensive copy (thay đổi list không ảnh hưởng manager)")
    void getAllRooms_isDefensiveCopy() {
        manager.addRoom(createAuction("item-C"));

        List<Auction> rooms = manager.getAllRooms();
        int originalSize = rooms.size();

        // Xóa khỏi list trả về
        rooms.clear();

        // Manager vẫn phải còn auction đó
        assertEquals(originalSize, manager.getAllRooms().size(),
                "Sửa list trả về từ getAllRooms không được ảnh hưởng đến AuctionManager");
    }
}

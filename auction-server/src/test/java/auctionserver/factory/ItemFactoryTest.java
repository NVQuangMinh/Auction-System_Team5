package auctionserver.factory;

import auctionserver.entities.Item;
import auctionserver.entities.User;
import auctionserver.entities.items.Arts;
import auctionserver.entities.items.Electronics;
import auctionserver.entities.items.Vehicles;
import auctionshared.dto.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho ItemFactory và các concrete factory.
 *
 * Trong hệ thống, ItemFactory được dùng ở hai nơi chính:
 *   - MessageHandlerService.handleSell(): tạo Item entity từ ItemDTO gửi lên
 *   - AuctionDAO: tạo lại Item entity khi đọc từ database (mapRowToItem)
 *
 * Factory Pattern ở đây đảm bảo mỗi ItemType (ARTS/ELECTRONICS/VEHICLES)
 * tạo đúng loại Item với typeSpecificAttribute tương ứng (artist/model/brand).
 */
@DisplayName("ItemFactory Tests")
class ItemFactoryTest {

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User("owner-id", "seller", "pass", "USER", "AVAILABLE");
    }

    // ════════════════════════════════════════════════════════════════════════
    // ItemFactory.of() — dispatch đúng factory theo ItemType
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("of(ARTS) - Tạo item đúng loại Arts với artistName")
    void of_ARTS_createsArtsItem() {
        Item<String> item = ItemFactory.of(ItemType.ARTS)
                .create("art-001", "Mona Lisa", "Famous painting", owner, "Da Vinci");

        // Đúng class
        assertInstanceOf(Arts.class, item,
                "Factory ARTS phải tạo instance của Arts");
        // Đúng type enum
        assertEquals(ItemType.ARTS, item.getType());
        // typeSpecificAttribute là artistName
        assertEquals("Da Vinci", item.getTypeSpecificAttribute(),
                "typeSpecificAttribute phải là artistName với Arts");
        // Label đúng
        assertEquals("Hoạ sĩ", item.getTypeAttributeLabel());
        // Các field cơ bản đúng
        assertEquals("art-001",         item.getId());
        assertEquals("Mona Lisa",       item.getName());
        assertEquals("Famous painting", item.getDescription());
        assertSame(owner,               item.getOwner());
    }

    @Test
    @DisplayName("of(ELECTRONICS) - Tạo item đúng loại Electronics với model")
    void of_ELECTRONICS_createsElectronicsItem() {
        Item<String> item = ItemFactory.of(ItemType.ELECTRONICS)
                .create("elec-001", "iPhone 15", "Apple phone", owner, "A17 Pro");

        assertInstanceOf(Electronics.class, item,
                "Factory ELECTRONICS phải tạo instance của Electronics");
        assertEquals(ItemType.ELECTRONICS, item.getType());
        assertEquals("A17 Pro", item.getTypeSpecificAttribute(),
                "typeSpecificAttribute phải là model với Electronics");
        assertEquals("Mẫu mã", item.getTypeAttributeLabel());
        assertEquals("iPhone 15", item.getName());
    }

    @Test
    @DisplayName("of(VEHICLES) - Tạo item đúng loại Vehicles với brand")
    void of_VEHICLES_createsVehiclesItem() {
        Item<String> item = ItemFactory.of(ItemType.VEHICLES)
                .create("veh-001", "Mustang GT", "Classic car", owner, "Ford");

        assertInstanceOf(Vehicles.class, item,
                "Factory VEHICLES phải tạo instance của Vehicles");
        assertEquals(ItemType.VEHICLES, item.getType());
        assertEquals("Ford", item.getTypeSpecificAttribute(),
                "typeSpecificAttribute phải là brand với Vehicles");
        assertEquals("Thương hiệu", item.getTypeAttributeLabel());
        assertEquals("Mustang GT", item.getName());
    }

    // ════════════════════════════════════════════════════════════════════════
    // setTypeSpecificAttribute — dùng khi update thông tin item
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Electronics.setTypeSpecificAttribute - Cập nhật model mới")
    void electronics_setTypeSpecificAttribute_updatesModel() {
        Item<String> item = ItemFactory.of(ItemType.ELECTRONICS)
                .create("elec-002", "Galaxy S25", "Samsung", owner, "Exynos 2500");

        item.setTypeSpecificAttribute("Snapdragon 8 Elite");

        assertEquals("Snapdragon 8 Elite", item.getTypeSpecificAttribute());
    }
}

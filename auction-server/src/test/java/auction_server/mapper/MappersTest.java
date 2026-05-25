package auction_server.mapper;

import auction_server.entities.Auction;
import auction_server.entities.BidTransaction;
import auction_server.entities.User;
import auction_server.entities.items.Arts;
import auction_shared.dto.AuctionDTO;
import auction_shared.dto.AuctionStatus;
import auction_shared.dto.BidTransactionDTO;
import auction_shared.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho Mappers — lớp chuyển đổi giữa Entity (server) và DTO (shared).
 *
 * Trong hệ thống, Mappers được dùng ở nhiều nơi:
 *   - MessageHandlerService: convert trước khi gửi qua mạng
 *   - AuctionScheduler: convert AuctionDTO để broadcast khi kết thúc
 *   - AdminControlPanelController nhận DTO và hiển thị lên TableView
 *
 * Test đảm bảo không có field nào bị bỏ sót hay bị nhầm khi map.
 */
@DisplayName("Mappers Tests")
class MappersTest {

    // ── Fixture chung ────────────────────────────────────────────────────────
    private User seller;
    private Arts item;
    private Auction auction;
    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        seller  = new User("seller-id", "seller", "pass", "ADMIN", "AVAILABLE");
        item    = new Arts("item-001", "Mona Lisa", "Famous painting", seller, "Da Vinci");
        start   = LocalDateTime.now().minusDays(1);
        end     = LocalDateTime.now().plusDays(1);
        auction = new Auction(item, 100.0, 1000.0, 50.0, start, end, true);
    }

    // ════════════════════════════════════════════════════════════════════════
    // toDTO(User)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("toDTO(User) - Map đúng id, username, role")
    void toDTO_user_mapsIdUsernameRole() {
        UserDTO dto = Mappers.toDTO(seller);

        assertNotNull(dto);
        assertEquals("seller-id", dto.getId());
        assertEquals("seller",    dto.getUsername());
        assertEquals("ADMIN",     dto.getRole());
    }

    @Test
    @DisplayName("toDTO(User null) - Trả về null")
    void toDTO_nullUser_returnsNull() {
        assertNull(Mappers.toDTO((User) null));
    }

    // ════════════════════════════════════════════════════════════════════════
    // toDTO(Auction)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("toDTO(Auction) - Map đúng status, prices, times, antiSniping")
    void toDTO_auction_mapsAllFields() {
        AuctionDTO dto = Mappers.toDTO(auction);

        assertNotNull(dto);
        // Status ban đầu là ACTIVE
        assertEquals(AuctionStatus.ACTIVE, dto.getStatus());
        assertEquals(100.0, dto.getStartingPrice());
        assertEquals(1000.0, dto.getBuyOutPrice());
        assertEquals(50.0, dto.getTickSize());
        assertEquals(start, dto.getStartTime());
        assertEquals(end,   dto.getEndTime());
        assertTrue(dto.isAntiSniping(), "antiSniping phải được map đúng (true)");
        // Item bên trong DTO phải chứa đúng thông tin
        assertEquals("item-001", dto.getItem().getId());
        assertEquals("Mona Lisa", dto.getItem().getName());
    }

    @Test
    @DisplayName("toDTO(Auction) - winnerId phản ánh đúng sau khi endAuction")
    void toDTO_auction_mapsWinnerId_afterEnd() throws Exception {
        // Tạo auction với bidder
        User buyer = new User("buyer-id", "buyer", "p", "USER", "AVAILABLE");
        Auction auctionWithBid = new Auction(
                item, 100.0, 1000.0, 50.0, start, end, false
        );
        auctionWithBid.placeBid(new BidTransaction(auctionWithBid, buyer, 150.0));
        auctionWithBid.endAuction();

        AuctionDTO dto = Mappers.toDTO(auctionWithBid);

        assertEquals("buyer-id", dto.getWinnerId(),
                "winnerId trong DTO phải khớp với bidder sau khi auction kết thúc");
    }

    @Test
    @DisplayName("toDTO(Auction null) - Trả về null")
    void toDTO_nullAuction_returnsNull() {
        assertNull(Mappers.toDTO((Auction) null));
    }

    // ════════════════════════════════════════════════════════════════════════
    // toAuctionDTOList
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("toAuctionDTOList - List 3 auction → List 3 DTO đúng thứ tự")
    void toAuctionDTOList_mapsCorrectSize() {
        Arts item2 = new Arts("item-002", "Starry Night", "Van Gogh", seller, "Van Gogh");
        Arts item3 = new Arts("item-003", "Scream", "Munch", seller, "Munch");
        Auction a2 = new Auction(item2, 200.0, 2000.0, 100.0, start, end, false);
        Auction a3 = new Auction(item3, 300.0, 3000.0, 150.0, start, end, false);

        List<AuctionDTO> dtos = Mappers.toAuctionDTOList(Arrays.asList(auction, a2, a3));

        assertEquals(3, dtos.size());
        assertEquals("item-001", dtos.get(0).getItem().getId());
        assertEquals("item-002", dtos.get(1).getItem().getId());
        assertEquals("item-003", dtos.get(2).getItem().getId());
    }

    @Test
    @DisplayName("toAuctionDTOList - List rỗng → trả về list rỗng (không null)")
    void toAuctionDTOList_empty_returnsEmptyList() {
        List<AuctionDTO> dtos = Mappers.toAuctionDTOList(Collections.emptyList());

        assertNotNull(dtos);
        assertTrue(dtos.isEmpty());
    }

    // ════════════════════════════════════════════════════════════════════════
    // toBidTransactionDTOList
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("toBidTransactionDTOList - 2 transaction → 2 DTO với bidAmount đúng")
    void toBidTransactionDTOList_mapsCorrectly() throws Exception {
        User buyer = new User("buyer-id", "buyer", "p", "USER", "AVAILABLE");
        BidTransaction tx1 = new BidTransaction(auction, buyer, 150.0);
        BidTransaction tx2 = new BidTransaction(auction, buyer, 200.0);

        List<BidTransactionDTO> dtos = Mappers.toBidTransactionDTOList(Arrays.asList(tx1, tx2));

        assertEquals(2, dtos.size());
        assertEquals(150.0, dtos.get(0).getBidAmount());
        assertEquals(200.0, dtos.get(1).getBidAmount());
    }
}

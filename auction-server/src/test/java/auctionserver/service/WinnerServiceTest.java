package auctionserver.service;

import auctionserver.dao.DAOProvider;
import auctionserver.dao.UserDAO;
import auctionserver.entities.BidTransaction;
import auctionserver.entities.User;
import auctionserver.entities.Auction;
import auctionserver.entities.items.Arts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho WinnerService.
 *
 * WinnerService xác định winner hợp lệ từ danh sách bid history
 * bằng cách truy vấn trạng thái thời gian thực từ cơ sở dữ liệu.
 */
@DisplayName("WinnerService Tests")
class WinnerServiceTest {

    private WinnerService winnerService;
    private DAOProvider daoProvider;
    private UserDAO userDAO;

    // Fixture: các User với trạng thái khác nhau
    private User validBidder1;    // bidder hợp lệ, bid sớm
    private User validBidder2;    // bidder hợp lệ, bid sau
    private User bannedBidder;    // bidder bị ban
    private Auction dummyAuction; // auction giả, chỉ để tạo BidTransaction

    @BeforeEach
    void setUp() {
        daoProvider = mock(DAOProvider.class);
        userDAO = mock(UserDAO.class);
        when(daoProvider.userDAO()).thenReturn(userDAO);

        winnerService = new WinnerService(daoProvider);

        validBidder1 = new User("v1", "nam", "p", "USER", "AVAILABLE");
        validBidder2 = new User("v2", "minh",   "p", "USER", "AVAILABLE");
        bannedBidder = new User("b1", "banned", "p", "USER", "BANNED");

        // Auction giả: chỉ để giữ reference trong BidTransaction
        Arts item = new Arts("i1", "Test Item", "desc",
                new User("s1", "seller", "p", "USER", "AVAILABLE"), "Artist");
        dummyAuction = new Auction(
                item, 100.0, 1000.0, 50.0,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                false
        );
    }

    // ── Helper: tạo BidTransaction với bidder và amount cho trước ────────────
    private BidTransaction bid(User bidder, double amount) {
        return new BidTransaction(dummyAuction, bidder, amount);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Các trường hợp cơ bản
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("History bình thường → trả về bidder của bid cuối cùng")
    void determineWinner_normalHistory_returnsLastBidder() {
        List<BidTransaction> history = Arrays.asList(
                bid(validBidder1, 150.0),
                bid(validBidder2, 200.0)
        );

        when(userDAO.getUserByUsername("nam")).thenReturn(validBidder1);
        when(userDAO.getUserByUsername("minh")).thenReturn(validBidder2);

        String winnerId = winnerService.determineWinner(history);

        assertEquals(validBidder2.getId(), winnerId,
                "Winner phải là người bid cao nhất (cuối cùng trong list)");
    }

    @Test
    @DisplayName("Chỉ có 1 bid → trả về bidder duy nhất đó")
    void determineWinner_singleBid_returnsThatBidder() {
        List<BidTransaction> history = Collections.singletonList(
                bid(validBidder1, 150.0)
        );

        when(userDAO.getUserByUsername("nam")).thenReturn(validBidder1);

        assertEquals(validBidder1.getId(), winnerService.determineWinner(history));
    }

    // ════════════════════════════════════════════════════════════════════════
    // Trường hợp biên: list rỗng / null
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("History rỗng → null (không có winner)")
    void determineWinner_emptyHistory_returnsNull() {
        assertNull(winnerService.determineWinner(Collections.emptyList()),
                "Không có bid nào → không có winner");
    }

    @Test
    @DisplayName("History = null → null")
    void determineWinner_nullHistory_returnsNull() {
        assertNull(winnerService.determineWinner(null),
                "null input phải được xử lý an toàn");
    }

    // ════════════════════════════════════════════════════════════════════════
    // Bidder bị BANNED — skip và tìm bidder hợp lệ trước đó
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Bidder cuối bị BANNED → skip, trả về bidder hợp lệ kế tiếp")
    void determineWinner_lastBidderBanned_skipsToNext() {
        List<BidTransaction> history = Arrays.asList(
                bid(validBidder1, 150.0),
                bid(bannedBidder, 200.0)
        );

        when(userDAO.getUserByUsername("nam")).thenReturn(validBidder1);
        when(userDAO.getUserByUsername("banned")).thenReturn(bannedBidder);

        String winnerId = winnerService.determineWinner(history);

        assertEquals(validBidder1.getId(), winnerId,
                "Bỏ qua bidder bị ban, winner phải là người bid hợp lệ gần nhất");
    }

    @Test
    @DisplayName("Tất cả bidder đều BANNED → null")
    void determineWinner_allBiddersBanned_returnsNull() {
        User bannedBidder2 = new User("b2", "banned2", "p", "USER", "BANNED");

        List<BidTransaction> history = Arrays.asList(
                bid(bannedBidder,  150.0),
                bid(bannedBidder2, 200.0)
        );

        when(userDAO.getUserByUsername("banned")).thenReturn(bannedBidder);
        when(userDAO.getUserByUsername("banned2")).thenReturn(bannedBidder2);

        assertNull(winnerService.determineWinner(history),
                "Tất cả bidder bị ban → không có winner hợp lệ");
    }

    // ════════════════════════════════════════════════════════════════════════
    // BidTransaction có bidder = null
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("BidTransaction cuối có bidder = null → bỏ qua, lấy bidder hợp lệ trước")
    void determineWinner_nullBidder_skipsEntry() {
        BidTransaction nullBidderTx = new BidTransaction(dummyAuction, null, 200.0);

        List<BidTransaction> history = Arrays.asList(
                bid(validBidder1, 150.0),
                nullBidderTx
        );

        when(userDAO.getUserByUsername("nam")).thenReturn(validBidder1);

        assertEquals(validBidder1.getId(), winnerService.determineWinner(history),
                "BidTransaction có bidder null phải bị bỏ qua");
    }

    @Test
    @DisplayName("Tất cả BidTransaction đều có bidder = null → null")
    void determineWinner_allNullBidders_returnsNull() {
        BidTransaction tx1 = new BidTransaction(dummyAuction, null, 150.0);
        BidTransaction tx2 = new BidTransaction(dummyAuction, null, 200.0);

        assertNull(winnerService.determineWinner(Arrays.asList(tx1, tx2)));
    }

    // ════════════════════════════════════════════════════════════════════════
    // Kết hợp: null bidder xen kẽ với valid bidder
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Mix: banned + null + valid → trả về bidder hợp lệ đầu tiên từ cuối")
    void determineWinner_mixed_bannedAndNull_returnsFirstValid() {
        BidTransaction nullBidderTx = new BidTransaction(dummyAuction, null, 300.0);

        List<BidTransaction> history = Arrays.asList(
                bid(validBidder1, 150.0),   // hợp lệ
                bid(bannedBidder, 200.0),   // bị ban
                nullBidderTx                // null bidder
        );

        when(userDAO.getUserByUsername("nam")).thenReturn(validBidder1);
        when(userDAO.getUserByUsername("banned")).thenReturn(bannedBidder);
        when(userDAO.getUserByUsername("nam")).thenReturn(validBidder1);

        assertEquals(validBidder1.getId(), winnerService.determineWinner(history));
    }

    // ════════════════════════════════════════════════════════════════════════
    // Trường hợp thực tế: Bidder có status AVAILABLE trên RAM nhưng BANNED trên DB
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Bidder có status AVAILABLE trên RAM nhưng bị BANNED trên DB → skip")
    void determineWinner_lastBidderBannedInDB_skipsToNext() {
        // nam bid trước (AVAILABLE), vuminh bid sau (AVAILABLE trên RAM tại thời điểm bid)
        User bidderBannedInDBButAvailableOnRam = new User("b1", "vuminh", "p", "USER", "AVAILABLE");

        List<BidTransaction> history = Arrays.asList(
                bid(validBidder1, 150.0),
                bid(bidderBannedInDBButAvailableOnRam, 200.0)
        );

        // Giả lập: vuminh sau đó bị Ban trên DB
        User bannedDbUser = new User("b1", "vuminh", "p", "USER", "BANNED");
        when(userDAO.getUserByUsername("nam")).thenReturn(validBidder1);
        when(userDAO.getUserByUsername("vuminh")).thenReturn(bannedDbUser);

        String winnerId = winnerService.determineWinner(history);

        assertEquals(validBidder1.getId(), winnerId,
                "Phải bỏ qua bidder bị BANNED trên DB mặc dù trên RAM là AVAILABLE");
    }
}

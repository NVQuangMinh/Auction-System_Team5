package auction_server.service;

import auction_server.entities.BidTransaction;
import auction_server.entities.User;
import auction_server.entities.Auction;
import auction_server.entities.items.Arts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho WinnerService.
 *
 * WinnerService có một nhiệm vụ duy nhất: xác định winner hợp lệ
 * từ danh sách bid history khi auction kết thúc.
 *
 * Business rule (từ code thực):
 *   1. Duyệt từ cuối list lên đầu (bid mới nhất trước)
 *   2. Bỏ qua entry có bidder = null
 *   3. Bỏ qua bidder có userStatus = "BANNED"
 *   4. Trả về ID của bidder hợp lệ đầu tiên tìm được
 *   5. Nếu không tìm được → trả null (không có winner)
 *
 */
@DisplayName("WinnerService Tests")
class WinnerServiceTest {

    private WinnerService winnerService;

    // Fixture: các User với trạng thái khác nhau
    private User validBidder1;    // bidder hợp lệ, bid sớm
    private User validBidder2;    // bidder hợp lệ, bid sau
    private User bannedBidder;    // bidder bị ban
    private Auction dummyAuction; // auction giả, chỉ để tạo BidTransaction

    @BeforeEach
    void setUp() {
        // WinnerService không dùng DAOProvider trong determineWinner()
        // → truyền null hoàn toàn an toàn cho mục đích test
        winnerService = new WinnerService(null);

        validBidder1 = new User("v1", "alice", "p", "USER", "AVAILABLE");
        validBidder2 = new User("v2", "bob",   "p", "USER", "AVAILABLE");
        bannedBidder = new User("b1", "charlie", "p", "USER", "BANNED");

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
    @DisplayName("✅ History bình thường → trả về bidder của bid cuối cùng")
    void determineWinner_normalHistory_returnsLastBidder() {
        // validBidder1 bid trước, validBidder2 bid sau → winner = validBidder2
        List<BidTransaction> history = Arrays.asList(
                bid(validBidder1, 150.0),
                bid(validBidder2, 200.0)
        );

        String winnerId = winnerService.determineWinner(history);

        assertEquals(validBidder2.getId(), winnerId,
                "Winner phải là người bid cao nhất (cuối cùng trong list)");
    }

    @Test
    @DisplayName("✅ Chỉ có 1 bid → trả về bidder duy nhất đó")
    void determineWinner_singleBid_returnsThatBidder() {
        List<BidTransaction> history = Collections.singletonList(
                bid(validBidder1, 150.0)
        );

        assertEquals(validBidder1.getId(), winnerService.determineWinner(history));
    }

    // ════════════════════════════════════════════════════════════════════════
    // Trường hợp biên: list rỗng / null
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("❌ History rỗng → null (không có winner)")
    void determineWinner_emptyHistory_returnsNull() {
        assertNull(winnerService.determineWinner(Collections.emptyList()),
                "Không có bid nào → không có winner");
    }

    @Test
    @DisplayName("❌ History = null → null")
    void determineWinner_nullHistory_returnsNull() {
        assertNull(winnerService.determineWinner(null),
                "null input phải được xử lý an toàn, không NullPointerException");
    }

    // ════════════════════════════════════════════════════════════════════════
    // Bidder bị BANNED — skip và tìm bidder hợp lệ trước đó
    // (phản ánh rule: nếu winner bị admin ban sau khi đặt giá, phải tìm người khác)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("⚠️ Bidder cuối bị BANNED → skip, trả về bidder hợp lệ kế tiếp")
    void determineWinner_lastBidderBanned_skipsToNext() {
        // validBidder1 bid trước (hợp lệ), bannedBidder bid sau (bị ban)
        List<BidTransaction> history = Arrays.asList(
                bid(validBidder1, 150.0),
                bid(bannedBidder, 200.0)
        );

        String winnerId = winnerService.determineWinner(history);

        assertEquals(validBidder1.getId(), winnerId,
                "Bỏ qua bidder bị ban, winner phải là người bid hợp lệ gần nhất");
    }

    @Test
    @DisplayName("⚠️ 2 bidder cuối đều BANNED → trả về bidder hợp lệ sớm hơn")
    void determineWinner_lastTwoBiddersBanned_returnsEarlierValid() {
        User bannedBidder2 = new User("b2", "dave", "p", "USER", "BANNED");

        List<BidTransaction> history = Arrays.asList(
                bid(validBidder1, 150.0),  // hợp lệ
                bid(bannedBidder,  200.0),  // bị ban
                bid(bannedBidder2, 250.0)   // bị ban
        );

        assertEquals(validBidder1.getId(), winnerService.determineWinner(history));
    }

    @Test
    @DisplayName("❌ Tất cả bidder đều BANNED → null")
    void determineWinner_allBiddersBanned_returnsNull() {
        User bannedBidder2 = new User("b2", "dave", "p", "USER", "BANNED");

        List<BidTransaction> history = Arrays.asList(
                bid(bannedBidder,  150.0),
                bid(bannedBidder2, 200.0)
        );

        assertNull(winnerService.determineWinner(history),
                "Tất cả bidder bị ban → không có winner hợp lệ");
    }

    // ════════════════════════════════════════════════════════════════════════
    // BidTransaction có bidder = null (trường hợp data bất thường từ DB)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("⚠️ BidTransaction cuối có bidder = null → bỏ qua, lấy bidder hợp lệ trước")
    void determineWinner_nullBidder_skipsEntry() {
        BidTransaction nullBidderTx = new BidTransaction(dummyAuction, null, 200.0);

        List<BidTransaction> history = Arrays.asList(
                bid(validBidder1, 150.0),
                nullBidderTx              // bidder null — có thể xảy ra khi data DB lỗi
        );

        assertEquals(validBidder1.getId(), winnerService.determineWinner(history),
                "BidTransaction có bidder null phải bị bỏ qua");
    }

    @Test
    @DisplayName("❌ Tất cả BidTransaction đều có bidder = null → null")
    void determineWinner_allNullBidders_returnsNull() {
        BidTransaction tx1 = new BidTransaction(dummyAuction, null, 150.0);
        BidTransaction tx2 = new BidTransaction(dummyAuction, null, 200.0);

        assertNull(winnerService.determineWinner(Arrays.asList(tx1, tx2)));
    }

    // ════════════════════════════════════════════════════════════════════════
    // Kết hợp: null bidder xen kẽ với valid bidder
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("⚠️ Mix: banned + null + valid → trả về bidder hợp lệ đầu tiên từ cuối")
    void determineWinner_mixed_bannedAndNull_returnsFirstValid() {
        BidTransaction nullBidderTx = new BidTransaction(dummyAuction, null, 300.0);

        List<BidTransaction> history = Arrays.asList(
                bid(validBidder1, 150.0),   // hợp lệ
                bid(bannedBidder, 200.0),   // bị ban
                nullBidderTx               // null bidder
        );

        // Duyệt từ cuối: null(skip) → banned(skip) → validBidder1(✅)
        assertEquals(validBidder1.getId(), winnerService.determineWinner(history));
    }
}

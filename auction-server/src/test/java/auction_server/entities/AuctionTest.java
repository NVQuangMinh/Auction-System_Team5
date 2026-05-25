package auction_server.entities;

import auction_server.entities.items.Arts;
import auction_server.exception.BidException;
import auction_server.exception.InvalidBidAmountException;
import auction_shared.dto.AuctionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho Auction entity.
 *
 * Auction là core domain object của hệ thống:
 * - Quản lý trạng thái (ACTIVE → ENDED/SOLD)
 * - Lưu lịch sử bid trong RAM
 * - Hỗ trợ revert khi DB transaction thất bại
 * - Anti-sniping: gia hạn thời gian khi bid vào phút cuối
 *
 */
@DisplayName("Auction Entity Tests")
class AuctionTest {

    private static final double STARTING_PRICE = 100.0;
    private static final double BUY_OUT_PRICE = 1000.0;
    private static final double TICK_SIZE = 50.0;

    private User seller;
    private User buyer;
    private Item<String> item;
    private Auction auction;

    @BeforeEach
    void setUp() {
        seller = new User("seller-id", "seller", "123", "USER", "AVAILABLE");
        buyer = new User("buyer-id", "buyer", "456", "USER", "AVAILABLE");
        item = new Arts("item-001", "Mona Lisa", "Famous painting", seller, "Da Vinci");
        auction = new Auction(
                item,
                STARTING_PRICE,
                BUY_OUT_PRICE,
                TICK_SIZE,
                LocalDateTime.now().minusHours(1), // startTime = 1 giờ trước
                LocalDateTime.now().plusHours(2), // endTime = 2 giờ sau
                false
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Tạo BidTransaction hợp lệ với bid amount cho trước. */
    private BidTransaction validBid(double amount) {
        return new BidTransaction(auction, buyer, amount);
    }

    // ════════════════════════════════════════════════════════════════════════
    // getCurrentHighestBid
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getCurrentHighestBid - Chưa có bid nào → trả về startingPrice")
    void getCurrentHighestBid_noHistory_returnsStartingPrice() {
        assertEquals(STARTING_PRICE, auction.getCurrentHighestBid());
    }

    @Test
    @DisplayName("getCurrentHighestBid - Có 2 bid → trả về bid cuối cùng")
    void getCurrentHighestBid_withBids_returnsLast() throws BidException {

        auction.placeBid(new BidTransaction(auction, buyer, 150.0));

        User buyer2 = new User("b2", "buyer2", "p", "USER", "AVAILABLE");
        auction.placeBid(new BidTransaction(auction, buyer2, 200.0));

        assertEquals(200.0, auction.getCurrentHighestBid());
    }

    // ════════════════════════════════════════════════════════════════════════
    // placeBid
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("placeBid - Bid hợp lệ (đúng tick) → thêm vào bidHistory")
    void placeBid_valid_appendsToBidHistory() throws BidException {
        auction.placeBid(validBid(150.0));

        assertEquals(1, auction.getBidHistory().size());
        assertEquals(150.0, auction.getCurrentHighestBid());
    }

    @Test
    @DisplayName("placeBid - Bid không hợp lệ → throw exception, bidHistory không thay đổi")
    void placeBid_invalid_doesNotModifyHistory() {
        assertThrows(InvalidBidAmountException.class,
                () -> auction.placeBid(validBid(110.0)));

        assertTrue(auction.getBidHistory().isEmpty(),
                "bidHistory phải vẫn rỗng sau bid thất bại");
        assertEquals(STARTING_PRICE, auction.getCurrentHighestBid());
    }

    // ════════════════════════════════════════════════════════════════════════
    // revertLastBid — cơ chế bảo vệ khi DB transaction thất bại
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("revertLastBid - Bid 1 lần rồi revert → history rỗng, bid = startingPrice")
    void revertLastBid_singleBid_restoresToStartingPrice() throws BidException {
        BidTransaction tx = validBid(150.0);
        auction.placeBid(tx);

        auction.revertLastBid(tx);

        assertTrue(auction.getBidHistory().isEmpty());
        assertEquals(STARTING_PRICE, auction.getCurrentHighestBid());
    }

    @Test
    @DisplayName("revertLastBid - Bid 2 lần, revert lần 2 → history còn 1, bid = 150")
    void revertLastBid_withPreviousBid_restoresToPreviousBid() throws BidException {
        BidTransaction tx1 = validBid(150.0);
        auction.placeBid(tx1);

        User buyer2 = new User("b2", "buyer2", "p", "USER", "AVAILABLE");
        BidTransaction tx2 = new BidTransaction(auction, buyer2, 200.0);
        auction.placeBid(tx2);

        auction.revertLastBid(tx2);

        assertEquals(1, auction.getBidHistory().size());
        assertEquals(150.0, auction.getCurrentHighestBid());
    }

    @Test
    @DisplayName("revertLastBid - History rỗng → không crash (no-op)")
    void revertLastBid_emptyHistory_noOp() {
        BidTransaction tx = validBid(150.0);
        // Gọi revert mà không có bid → không throw
        assertDoesNotThrow(() -> auction.revertLastBid(tx));
        assertEquals(STARTING_PRICE, auction.getCurrentHighestBid());
    }

    // ════════════════════════════════════════════════════════════════════════
    // endAuction
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("endAuction - Auction ACTIVE → status chuyển thành ENDED")
    void endAuction_active_setsStatusEnded() {
        auction.endAuction();

        assertEquals(AuctionStatus.ENDED, auction.getStatus());
    }

    @Test
    @DisplayName("endAuction - Có bid → winnerId = ID của bidder cuối")
    void endAuction_withBid_setsWinnerId() throws BidException {
        auction.placeBid(validBid(150.0));
        auction.endAuction();

        assertEquals(buyer.getId(), auction.getWinnerId());
    }

    @Test
    @DisplayName("endAuction - Không có bid → winnerId = null")
    void endAuction_noBid_winnerIdNull() {
        auction.endAuction();

        assertNull(auction.getWinnerId());
    }


    // ════════════════════════════════════════════════════════════════════════
    // buyOut + revertBuyOut
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("buyOut - BuyOut đúng giá → status = SOLD, winnerId = buyerId")
    void buyOut_valid_setsSOLDAndWinner() throws BidException {
        BidTransaction buyOutTx = new BidTransaction(auction, buyer, BUY_OUT_PRICE);
        auction.buyOut(buyOutTx);

        assertEquals(AuctionStatus.SOLD, auction.getStatus());
        assertEquals(buyer.getId(), auction.getWinnerId());
    }

    @Test
    @DisplayName("revertBuyOut - Sau khi buyOut thất bại DB → trả về ACTIVE, owner cũ")
    void revertBuyOut_restoresOwnerAndActiveStatus() throws BidException {
        User originalOwner = item.getOwner();
        BidTransaction buyOutTx = new BidTransaction(auction, buyer, BUY_OUT_PRICE);

        // Giả lập buyOut thành công trong RAM, rồi DB fail → revert
        auction.buyOut(buyOutTx);
        auction.revertBuyOut(buyOutTx);

        assertEquals(AuctionStatus.ACTIVE, auction.getStatus());
        assertNull(auction.getWinnerId());
        assertSame(originalOwner, item.getOwner(),
                "Owner phải trở lại là seller ban đầu sau khi revert");
    }

    // ════════════════════════════════════════════════════════════════════════
    // extendTime (anti-sniping)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("extendTime - endTime còn 20s (< 30s grace) → gia hạn thêm 30s")
    void extendTime_withinGrace_extendsEndTime() {
        Auction antiSnipeAuction = new Auction(
                item, STARTING_PRICE, BUY_OUT_PRICE, TICK_SIZE,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusSeconds(20), // còn 20s
                true);
        LocalDateTime before = antiSnipeAuction.getEndTime();

        antiSnipeAuction.extendTime();

        assertTrue(antiSnipeAuction.getEndTime().isAfter(before),
                "endTime phải được gia hạn");
    }

    @Test
    @DisplayName("extendTime - endTime còn 2 giờ (ngoài grace) → endTime không đổi")
    void extendTime_outsideGrace_noChange() {
        LocalDateTime originalEnd = auction.getEndTime();
        auction.extendTime();

        assertEquals(originalEnd, auction.getEndTime(),
                "endTime không được thay đổi khi còn nhiều thời gian");
    }

    @Test
    @DisplayName("extendTime - Auction đã ENDED → không gia hạn")
    void extendTime_alreadyEnded_noChange() {
        auction.endAuction(); // chuyển sang ENDED
        LocalDateTime originalEnd = auction.getEndTime();

        auction.extendTime();

        assertEquals(originalEnd, auction.getEndTime());
    }

    // ════════════════════════════════════════════════════════════════════════
    // isExpired
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("isExpired - endTime ở quá khứ → true")
    void isExpired_past_returnsTrue() {
        Auction expiredAuction = new Auction(
                item, STARTING_PRICE, BUY_OUT_PRICE, TICK_SIZE,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusSeconds(1), // đã hết hạn
                false);
        assertTrue(expiredAuction.isExpired());
    }

    @Test
    @DisplayName("isExpired - endTime ở tương lai → false")
    void isExpired_future_returnsFalse() {
        assertFalse(auction.isExpired());
    }
}

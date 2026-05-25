package auction_server.service;

import auction_server.entities.Auction;
import auction_server.entities.BidTransaction;
import auction_server.entities.User;
import auction_server.entities.items.Arts;
import auction_server.exception.InactiveBidException;
import auction_server.exception.InvalidBidAmountException;
import auction_server.exception.SelfBiddingException;
import auction_shared.dto.AuctionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho ValidatorService.
 *
 * ValidatorService chứa toàn bộ business rule của việc đặt giá:
 *   - validateBid():    kiểm tra bid thông thường (phải đúng tick, đúng range)
 *   - validateBuyOut(): kiểm tra mua ngay (phải đúng bằng buyOutPrice)
 */
@DisplayName("ValidatorService Tests")
class ValidatorServiceTest {

    private static final double STARTING_PRICE = 100.0;
    private static final double BUY_OUT_PRICE  = 500.0;
    private static final double TICK_SIZE      = 50.0;

    private User    seller;
    private User    buyer;
    private Arts    item;
    private Auction activeAuction;

    @BeforeEach
    void setUp() {
        seller = new User("seller-id", "seller", "pass", "USER", "AVAILABLE");
        buyer  = new User("buyer-id",  "buyer",  "pass", "USER", "AVAILABLE");
        item   = new Arts("item-001", "Mona Lisa", "desc", seller, "Da Vinci");

        activeAuction = new Auction(
                item, STARTING_PRICE, BUY_OUT_PRICE, TICK_SIZE,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(2),
                false
        );
    }

    private BidTransaction bid(double amount) {
        return new BidTransaction(activeAuction, buyer, amount);
    }

    // ════════════════════════════════════════════════════════════════════════
    // validateBid
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("validateBid")
    class ValidateBid {

        @Test
        @DisplayName("Bid hợp lệ: đúng 1 tick → không throw")
        void success_exactlyOneTick() {
            // 150 = 100 (startingPrice) + 50 (1 tick) — hợp lệ
            assertDoesNotThrow(() -> ValidatorService.validateBid(activeAuction, bid(150.0)));
        }

        // ── InactiveBidException ─────────────────────────────────────────────

        @Test
        @DisplayName("Auction đã ENDED → InactiveBidException")
        void fail_auctionEnded() {
            // Giả lập auction đã kết thúc bằng cách rebuild với status ENDED
            Auction ended = new Auction(
                    item, STARTING_PRICE, BUY_OUT_PRICE, TICK_SIZE,
                    LocalDateTime.now().minusHours(2),
                    LocalDateTime.now().minusHours(1),
                    false,
                    STARTING_PRICE,          // currentHighestBid
                    AuctionStatus.ENDED      // status
            );
            BidTransaction tx = new BidTransaction(ended, buyer, 150.0);

            assertThrows(InactiveBidException.class,
                    () -> ValidatorService.validateBid(ended, tx));
        }

        @Test
        @DisplayName("Auction đã hết hạn (endTime quá khứ) → InactiveBidException")
        void fail_auctionExpired() {
            Auction expired = new Auction(
                    item, STARTING_PRICE, BUY_OUT_PRICE, TICK_SIZE,
                    LocalDateTime.now().minusHours(2),
                    LocalDateTime.now().minusSeconds(1),   // hết hạn 1 giây trước
                    false
            );
            BidTransaction tx = new BidTransaction(expired, buyer, 150.0);

            assertThrows(InactiveBidException.class,
                    () -> ValidatorService.validateBid(expired, tx));
        }

        // ── SelfBiddingException ─────────────────────────────────────────────

        @Test
        @DisplayName("Bidder trùng username với seller → SelfBiddingException")
        void fail_selfBidding() {
            // seller đặt giá cho chính sản phẩm của mình
            BidTransaction selfBid = new BidTransaction(activeAuction, seller, 150.0);

            assertThrows(SelfBiddingException.class,
                    () -> ValidatorService.validateBid(activeAuction, selfBid));
        }

        // ── InvalidBidAmountException ────────────────────────────────────────

        @Test
        @DisplayName("bidAmount == currentHighestBid (không tăng) → InvalidBidAmountException")
        void fail_bidEqualToCurrentHighest() {
            // 100 = startingPrice = currentHighestBid khi chưa có bid
            assertThrows(InvalidBidAmountException.class,
                    () -> ValidatorService.validateBid(activeAuction, bid(100.0)));
        }

        @Test
        @DisplayName("bidAmount < currentHighestBid → InvalidBidAmountException")
        void fail_bidBelowCurrentHighest() {
            assertThrows(InvalidBidAmountException.class,
                    () -> ValidatorService.validateBid(activeAuction, bid(50.0)));
        }

        @Test
        @DisplayName("bidAmount == buyOutPrice → InvalidBidAmountException")
        void fail_bidEqualsBuyOut() {
            // 500 = buyOutPrice → phải dùng mua ngay, không phải bid thường
            assertThrows(InvalidBidAmountException.class,
                    () -> ValidatorService.validateBid(activeAuction, bid(500.0)));
        }

        @Test
        @DisplayName("bidAmount không phải bội của tick (125 = 100 + 0.5 tick) → InvalidBidAmountException")
        void fail_bidNotMultipleOfTick() {
            // 125 tăng 25 so với 100, nhưng tick = 50 → không hợp lệ
            assertThrows(InvalidBidAmountException.class,
                    () -> ValidatorService.validateBid(activeAuction, bid(125.0)));
        }

        @Test
        @DisplayName("bidAmount lệch nhỏ khỏi bội tick (150.01) → InvalidBidAmountException")
        void fail_bidTinyOffFromTick() {
            // 150.01: tăng 50.01 — xấp xỉ 1 tick nhưng sai > 0.001
            assertThrows(InvalidBidAmountException.class,
                    () -> ValidatorService.validateBid(activeAuction, bid(150.01)));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // validateBuyOut
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("validateBuyOut")
    class ValidateBuyOut {

        @Test
        @DisplayName("BuyOut đúng giá buyOutPrice → không throw")
        void success_exactBuyOutPrice() {
            BidTransaction tx = new BidTransaction(activeAuction, buyer, BUY_OUT_PRICE);
            assertDoesNotThrow(() -> ValidatorService.validateBuyOut(activeAuction, tx));
        }

        @Test
        @DisplayName("BuyOut với giá lệch trong dung sai 0.001 → không throw")
        void success_withinTolerance() {
            BidTransaction tx = new BidTransaction(activeAuction, buyer, 500.0005);
            assertDoesNotThrow(() -> ValidatorService.validateBuyOut(activeAuction, tx));
        }

        // ── InactiveBidException ─────────────────────────────────────────────

        @Test
        @DisplayName("Auction đã ENDED → InactiveBidException")
        void fail_auctionEnded() {
            Auction ended = new Auction(
                    item, STARTING_PRICE, BUY_OUT_PRICE, TICK_SIZE,
                    LocalDateTime.now().minusHours(2),
                    LocalDateTime.now().minusHours(1),
                    false, STARTING_PRICE, AuctionStatus.ENDED
            );
            BidTransaction tx = new BidTransaction(ended, buyer, BUY_OUT_PRICE);

            assertThrows(InactiveBidException.class,
                    () -> ValidatorService.validateBuyOut(ended, tx));
        }

        @Test
        @DisplayName("Auction đã hết hạn → InactiveBidException")
        void fail_auctionExpired() {
            Auction expired = new Auction(
                    item, STARTING_PRICE, BUY_OUT_PRICE, TICK_SIZE,
                    LocalDateTime.now().minusHours(2),
                    LocalDateTime.now().minusSeconds(1),
                    false
            );
            BidTransaction tx = new BidTransaction(expired, buyer, BUY_OUT_PRICE);

            assertThrows(InactiveBidException.class,
                    () -> ValidatorService.validateBuyOut(expired, tx));
        }

        // ── SelfBiddingException ─────────────────────────────────────────────

        @Test
        @DisplayName("❌ Seller mua lại sản phẩm của chính mình → SelfBiddingException")
        void fail_selfBuyOut() {
            BidTransaction selfBuy = new BidTransaction(activeAuction, seller, BUY_OUT_PRICE);

            assertThrows(SelfBiddingException.class,
                    () -> ValidatorService.validateBuyOut(activeAuction, selfBuy));
        }

        // ── InvalidBidAmountException ────────────────────────────────────────

        @Test
        @DisplayName("❌ BuyOut giá thấp hơn buyOutPrice → InvalidBidAmountException")
        void fail_buyOutPriceTooLow() {
            BidTransaction tx = new BidTransaction(activeAuction, buyer, 499.0);
            assertThrows(InvalidBidAmountException.class,
                    () -> ValidatorService.validateBuyOut(activeAuction, tx));
        }

        @Test
        @DisplayName("❌ BuyOut giá cao hơn buyOutPrice → InvalidBidAmountException")
        void fail_buyOutPriceTooHigh() {
            BidTransaction tx = new BidTransaction(activeAuction, buyer, 501.0);
            assertThrows(InvalidBidAmountException.class,
                    () -> ValidatorService.validateBuyOut(activeAuction, tx));
        }
    }
}

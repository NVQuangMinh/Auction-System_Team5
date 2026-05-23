package auction_server.service;

import auction_server.dao.AuctionDAO;
import auction_server.dao.BidTransactionDAO;
import auction_server.dao.DAOProvider;
import auction_server.entities.Auction;
import auction_server.entities.BidTransaction;
import auction_server.entities.User;
import auction_server.exception.BidException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UNIT TEST LÀ GÌ?
 * ================
 * Unit test là việc kiểm tra từng phần nhỏ (unit) của code một cách độc lập.
 * Mục đích: Đảm bảo mỗi method hoạt động đúng như mong đợi.
 * 
 * CÁC KHÁI NIỆM QUAN TRỌNG:
 * =========================
 * 1. @Mock: Tạo object giả (fake) để thay thế dependency thật
 *    - Ví dụ: Thay vì dùng database thật, ta dùng mock DAO
 * 
 * 2. @BeforeEach: Method chạy TRƯỚC MỖI test case
 *    - Dùng để setup dữ liệu test
 * 
 * 3. @Test: Đánh dấu method là một test case
 * 
 * 4. Assertions: Kiểm tra kết quả
 *    - assertEquals(expected, actual): Kiểm tra 2 giá trị bằng nhau
 *    - assertNotNull(value): Kiểm tra không null
 *    - assertThrows(Exception.class, () -> code): Kiểm tra có throw exception
 * 
 * 5. Mockito:
 *    - when(...).thenReturn(...): Giả lập kết quả trả về
 *    - verify(...): Kiểm tra method đã được gọi chưa
 */
@DisplayName("BidService Unit Tests")
public class BidServiceTest {

    // ============================================
    // PHẦN 1: KHAI BÁO DEPENDENCIES (Mock objects)
    // ============================================
    // 
    // LƯU Ý: Không dùng @Mock vì Java Module System gây conflict
    // Thay vào đó, tạo mock thủ công trong setUp()
    
    private DAOProvider daoProvider; // Giả lập DAOProvider
    private AuctionDAO auctionDAO; // Giả lập AuctionDAO
    private BidTransactionDAO bidTransactionDAO; // Giả lập BidTransactionDAO
    private BidService bidService; // Object thật mà ta muốn test

    // ============================================
    // PHẦN 2: SETUP - Chạy trước mỗi test
    // ============================================
    
    @BeforeEach
    void setUp() {
        // Tạo mock objects thủ công (không dùng @Mock annotation)
        daoProvider = mock(DAOProvider.class);
        auctionDAO = mock(AuctionDAO.class);
        bidTransactionDAO = mock(BidTransactionDAO.class);
        
        // Giả lập: Khi gọi daoProvider.auctionDAO() thì trả về mock auctionDAO
        when(daoProvider.auctionDAO()).thenReturn(auctionDAO);
        when(daoProvider.bidTransactionDAO()).thenReturn(bidTransactionDAO);
        
        // Tạo BidService thật với mock dependencies
        bidService = new BidService(daoProvider);
    }

    // ============================================
    // PHẦN 3: TEST CASES
    // ============================================

    /**
     * TEST CASE 1: Tìm winner ID thành công
     * 
     * KỊCH BẢN:
     * - Có một bid transaction với bidder
     * - Gọi findWinnerId()
     * - Kỳ vọng: Trả về ID của bidder
     */
    @Test
    @DisplayName("findWinnerId - Trả về winner ID khi có bid transaction")
    void testFindWinnerId_Success() {
        // ARRANGE (Chuẩn bị dữ liệu test)
        String auctionId = "auction-001";
        String expectedWinnerId = "user-123";
        
        // Tạo mock User
        User mockBidder = mock(User.class);
        when(mockBidder.getId()).thenReturn(expectedWinnerId);
        
        // Tạo mock BidTransaction
        BidTransaction mockTransaction = mock(BidTransaction.class);
        when(mockTransaction.getBidder()).thenReturn(mockBidder);
        
        // Giả lập: Khi gọi findTopBidderByAuction thì trả về mockTransaction
        when(bidTransactionDAO.findTopBidderByAuction(auctionId)).thenReturn(mockTransaction);
        
        // ACT (Thực hiện hành động cần test)
        String actualWinnerId = bidService.findWinnerId(auctionId);
        
        // ASSERT (Kiểm tra kết quả)
        assertEquals(expectedWinnerId, actualWinnerId, "Winner ID phải khớp với bidder ID");
        
        // Verify: Đảm bảo method findTopBidderByAuction đã được gọi đúng 1 lần
        verify(bidTransactionDAO, times(1)).findTopBidderByAuction(auctionId);
    }

    /**
     * TEST CASE 2: Không tìm thấy winner
     * 
     * KỊCH BẢN:
     * - Không có bid transaction nào
     * - Gọi findWinnerId()
     * - Kỳ vọng: Trả về null
     */
    @Test
    @DisplayName("findWinnerId - Trả về null khi không có bid transaction")
    void testFindWinnerId_NoBidTransaction() {
        // ARRANGE
        String auctionId = "auction-002";
        
        // Giả lập: Không có transaction nào -> trả về null
        when(bidTransactionDAO.findTopBidderByAuction(auctionId)).thenReturn(null);
        
        // ACT
        String actualWinnerId = bidService.findWinnerId(auctionId);
        
        // ASSERT
        assertNull(actualWinnerId, "Winner ID phải là null khi không có bid");
        verify(bidTransactionDAO, times(1)).findTopBidderByAuction(auctionId);
    }

    /**
     * TEST CASE 3: Bid transaction có nhưng bidder là null
     * 
     * KỊCH BẢN:
     * - Có bid transaction nhưng bidder = null
     * - Gọi findWinnerId()
     * - Kỳ vọng: Trả về null
     */
    @Test
    @DisplayName("findWinnerId - Trả về null khi bidder là null")
    void testFindWinnerId_BidderIsNull() {
        // ARRANGE
        String auctionId = "auction-003";
        
        BidTransaction mockTransaction = mock(BidTransaction.class);
        when(mockTransaction.getBidder()).thenReturn(null); // Bidder = null
        
        when(bidTransactionDAO.findTopBidderByAuction(auctionId)).thenReturn(mockTransaction);
        
        // ACT
        String actualWinnerId = bidService.findWinnerId(auctionId);
        
        // ASSERT
        assertNull(actualWinnerId, "Winner ID phải là null khi bidder null");
    }

    /**
     * LƯU Ý VỀ TEST processAndSaveBid VÀ processBuyOut:
     * ==================================================
     * 
     * Hai method này RẤT PHỨC TẠP vì:
     * 1. Cần mock Connection, DatabaseConnection (static method)
     * 2. Cần mock Auction object với lock mechanism
     * 3. Cần test transaction rollback
     * 
     * ĐỂ TEST CHÚNG, BẠN CẦN:
     * - Dùng PowerMock hoặc MockedStatic (Mockito 3.4+) để mock static methods
     * - Hoặc refactor code để inject DatabaseConnection (Dependency Injection)
     * 
     * VÍ DỤ HƯỚNG DẪN:
     * - Tạo interface ConnectionProvider
     * - Inject vào BidService thay vì gọi DatabaseConnection.getConnection() trực tiếp
     * - Trong test, mock ConnectionProvider
     * 
     * ĐÂY LÀ KIẾN THỨC NÂNG CAO, KHÔNG BẮT BUỘC CHO NGƯỜI MỚI BẮT ĐẦU.
     */
}

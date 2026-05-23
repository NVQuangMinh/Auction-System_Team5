package auction_server.service;

import auction_server.dao.AuctionDAO;
import auction_server.dao.DAOProvider;
import auction_server.dao.ItemDAO;
import auction_server.entities.Auction;
import auction_server.entities.Item;
import auction_server.exception.DatabaseException;
import auction_server.exception.TransactionFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho SellService.
 *
 * SellService.publishItemAndAuction() có 3 lớp logic cần test:
 *
 *   1. VALIDATION (thuần Java, không cần DB):
 *      - item hoặc auction là null       → IllegalArgumentException
 *      - startingPrice >= buyOutPrice    → IllegalArgumentException
 *
 *   2. DB TRANSACTION THÀNH CÔNG:
 *      - itemDAO.insert() + auctionDAO.insert() + conn.commit() → không throw gì
 *
 *   3. DB TRANSACTION THẤT BẠI:
 *      - insert() throw SQLException     → rollback + throw TransactionFailedException
 *      - getConnection() throw SQLException → throw DatabaseException
 *
 * KỸ THUẬT ĐẶC BIỆT TRONG FILE NÀY:
 * ====================================
 * SellService gọi auctionDAO.getConnection() để lấy Connection.
 * Ta mock AuctionDAO và giả lập getConnection() trả về mock Connection.
 * Điều này cho phép test DB transaction mà không cần database thật.
 */
@DisplayName("SellService Unit Tests")
class SellServiceTest {

    // ── Dependencies (mock) ──────────────────────────────────────────────────
    private DAOProvider daoProvider;
    private ItemDAO itemDAO;
    private AuctionDAO auctionDAO;
    private Connection mockConn; // mock Connection để test transaction

    // ── Object cần test ──────────────────────────────────────────────────────
    private SellService sellService;

    // ── Dữ liệu dùng chung ──────────────────────────────────────────────────
    private Item mockItem;
    private Auction mockAuction;

    @BeforeEach
    void setUp() throws SQLException {
        daoProvider = mock(DAOProvider.class);
        itemDAO     = mock(ItemDAO.class);
        auctionDAO  = mock(AuctionDAO.class);
        mockConn    = mock(Connection.class);

        when(daoProvider.itemDAO()).thenReturn(itemDAO);
        when(daoProvider.auctionDAO()).thenReturn(auctionDAO);

        // Giả lập getConnection() trả về mock Connection
        when(auctionDAO.getConnection()).thenReturn(mockConn);

        sellService = new SellService(daoProvider);

        // Tạo Item và Auction giả dùng chung cho các test
        mockItem    = mock(Item.class);
        when(mockItem.getId()).thenReturn("item-001");

        mockAuction = mock(Auction.class);
        when(mockAuction.getStartingPrice()).thenReturn(100.0);
        when(mockAuction.getBuyOutPrice()).thenReturn(500.0);
        when(mockAuction.getItem()).thenReturn(mockItem);
        when(mockAuction.getStartTime()).thenReturn(LocalDateTime.now());
        when(mockAuction.getEndTime()).thenReturn(LocalDateTime.now().plusDays(1));
    }

    // ════════════════════════════════════════════════════════════════════════
    // NHÓM 1: VALIDATION — không cần DB
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("publishItemAndAuction - item null → IllegalArgumentException")
    void testPublish_ItemNull_ThrowsIllegalArgument() {
        // assertThrows: kiểm tra rằng đoạn code bên trong PHẢI throw đúng loại exception
        assertThrows(IllegalArgumentException.class,
                () -> sellService.publishItemAndAuction(null, mockAuction));
    }

    @Test
    @DisplayName("publishItemAndAuction - auction null → IllegalArgumentException")
    void testPublish_AuctionNull_ThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> sellService.publishItemAndAuction(mockItem, null));
    }

    @Test
    @DisplayName("publishItemAndAuction - startingPrice == buyOutPrice → IllegalArgumentException")
    void testPublish_StartingPriceEqualsBuyOut_ThrowsIllegalArgument() {
        // Giả lập startingPrice = buyOutPrice = 100
        when(mockAuction.getStartingPrice()).thenReturn(100.0);
        when(mockAuction.getBuyOutPrice()).thenReturn(100.0);

        assertThrows(IllegalArgumentException.class,
                () -> sellService.publishItemAndAuction(mockItem, mockAuction));
    }

    @Test
    @DisplayName("publishItemAndAuction - startingPrice > buyOutPrice → IllegalArgumentException")
    void testPublish_StartingPriceGreaterThanBuyOut_ThrowsIllegalArgument() {
        // Giả lập startingPrice > buyOutPrice
        when(mockAuction.getStartingPrice()).thenReturn(600.0);
        when(mockAuction.getBuyOutPrice()).thenReturn(500.0);

        assertThrows(IllegalArgumentException.class,
                () -> sellService.publishItemAndAuction(mockItem, mockAuction));
    }

    // ════════════════════════════════════════════════════════════════════════
    // NHÓM 2: DB TRANSACTION THÀNH CÔNG
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("publishItemAndAuction - Thành công: insert item + auction, commit được gọi")
    void testPublish_Success() throws SQLException {
        // ARRANGE — itemDAO và auctionDAO không throw gì (mặc định mock không làm gì)

        // ACT — không được throw exception
        assertDoesNotThrow(() -> sellService.publishItemAndAuction(mockItem, mockAuction));

        // ASSERT — verify đúng thứ tự: insert item → insert auction → commit
        verify(itemDAO, times(1)).insert(eq(mockItem), eq(mockConn));
        verify(auctionDAO, times(1)).insert(eq(mockAuction), eq(mockConn));
        verify(mockConn, times(1)).commit();

        // rollback KHÔNG được gọi khi thành công
        verify(mockConn, never()).rollback();
    }

    // ════════════════════════════════════════════════════════════════════════
    // NHÓM 3: DB TRANSACTION THẤT BẠI
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("publishItemAndAuction - itemDAO.insert() throw SQLException → rollback + TransactionFailedException")
    void testPublish_ItemInsertFails_RollbackAndThrows() throws SQLException {
        // ARRANGE — giả lập itemDAO.insert() throw SQLException
        doThrow(new SQLException("insert item failed"))
                .when(itemDAO).insert(any(), any());

        // ACT & ASSERT
        assertThrows(TransactionFailedException.class,
                () -> sellService.publishItemAndAuction(mockItem, mockAuction));

        // rollback PHẢI được gọi khi có lỗi
        verify(mockConn, times(1)).rollback();
        verify(mockConn, never()).commit();
    }

    @Test
    @DisplayName("publishItemAndAuction - auctionDAO.insert() throw SQLException → rollback + TransactionFailedException")
    void testPublish_AuctionInsertFails_RollbackAndThrows() throws SQLException {
        // ARRANGE — itemDAO thành công, auctionDAO.insert() throw SQLException
        when(itemDAO.insert(any(), any(Connection.class))).thenReturn(1);
        doThrow(new SQLException("insert auction failed"))
                .when(auctionDAO).insert(any(Auction.class), any(Connection.class));

        // ACT & ASSERT
        assertThrows(TransactionFailedException.class,
                () -> sellService.publishItemAndAuction(mockItem, mockAuction));

        verify(mockConn, times(1)).rollback();
        verify(mockConn, never()).commit();
    }

    @Test
    @DisplayName("publishItemAndAuction - getConnection() throw SQLException → DatabaseException")
    void testPublish_GetConnectionFails_ThrowsDatabaseException() throws SQLException {
        // ARRANGE — giả lập không lấy được connection
        when(auctionDAO.getConnection()).thenThrow(new SQLException("connection failed"));

        // ACT & ASSERT
        assertThrows(DatabaseException.class,
                () -> sellService.publishItemAndAuction(mockItem, mockAuction));

        // Không có connection → insert và rollback đều không được gọi
        verify(itemDAO, never()).insert(any(), any());
        verify(mockConn, never()).rollback();
    }
}

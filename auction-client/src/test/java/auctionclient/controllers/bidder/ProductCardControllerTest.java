package auctionclient.controllers.bidder;

import auctionclient.Network.ClientService;
import auctionclient.UserSession;
import auctionclient.interfaces.HandleCardClicked;
import auctionshared.Network.NetworkMessage;
import auctionshared.dto.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.matcher.control.LabeledMatchers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductCardControllerTest extends ApplicationTest {

    private ProductCardController controller;
    private ClientService mockClientService;
    private MockedStatic<ClientService> mockedStaticClientService;

    // --- Dữ liệu dùng chung cho các test ---
    private final UserDTO testUser = new UserDTO("01", "testUser", "USER");

    private ItemDTO createItem(ItemType type, String typeSpecificAttr) {
        return new ItemDTO("item-01", "Laptop Gaming", "Mot chiec laptop gaming cao cap",
                testUser, type, typeSpecificAttr);
    }

    private AuctionDTO createAuction(ItemDTO item, AuctionStatus status, double currentBid, double buyOut) {
        return new AuctionDTO(item, status, 0, buyOut, 50,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                false, null, currentBid);
    }

    // -------------------------------------------------------------------------
    // Thiết lập & Dọn dẹp
    // -------------------------------------------------------------------------

    @AfterEach
    public void closeMock() {
        if (mockedStaticClientService != null) {
            mockedStaticClientService.close();
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        mockClientService = mock(ClientService.class);
        mockedStaticClientService = mockStatic(ClientService.class);
        mockedStaticClientService.when(() -> ClientService.getInstance()).thenReturn(mockClientService);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/auctionclient/ProductCard.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        stage.setScene(new Scene(root));
        stage.show();
    }

    // =========================================================================
    // 1. testInitialize — kiểm tra trạng thái khởi tạo mặc định
    // =========================================================================

    /**
     * Sau khi load FXML, controller phải tồn tại và typeSpecificLabel
     * phải bị ẩn (managed=false, visible=false) theo mặc định trong FXML.
     */
    @Test
    public void testInitialize_DefaultState() {
        assertNotNull(controller);

        Label typeSpecificLabel = lookup("#typeSpecificLabel").queryAs(Label.class);
        assertNotNull(typeSpecificLabel);
        assertFalse(typeSpecificLabel.isVisible());
        assertFalse(typeSpecificLabel.isManaged());
    }

    // =========================================================================
    // 2. testSetData — kiểm tra dữ liệu được gán lên UI đúng
    // =========================================================================

    /**
     * setData() phải gán itemName, itemState, currentPrice, buyOutPrice,
     * và description đúng với dữ liệu của AuctionDTO.
     */
    @Test
    public void testSetData_CorrectLabels() {
        ItemDTO item = createItem(ItemType.ELECTRONICS, null);
        AuctionDTO auction = createAuction(item, AuctionStatus.ACTIVE, 500, 2000);
        HandleCardClicked mockListener = mock(HandleCardClicked.class);

        interact(() -> controller.setData(auction, mockListener));

        FxAssert.verifyThat("#itemName", LabeledMatchers.hasText("Laptop Gaming"));
        FxAssert.verifyThat("#itemState", LabeledMatchers.hasText("ACTIVE"));
        FxAssert.verifyThat("#description", LabeledMatchers.hasText("Mot chiec laptop gaming cao cap"));
        FxAssert.verifyThat("#currentPrice", LabeledMatchers.hasText("500"));
        FxAssert.verifyThat("#buyOutPrice", LabeledMatchers.hasText("2,000"));
    }

    /**
     * Khi AuctionStatus là ENDED, itemState phải hiển thị "ENDED".
     */
    @Test
    public void testSetData_StatusEnded() {
        ItemDTO item = createItem(ItemType.ARTS, null);
        AuctionDTO auction = createAuction(item, AuctionStatus.ENDED, 300, 1500);
        HandleCardClicked mockListener = mock(HandleCardClicked.class);

        interact(() -> controller.setData(auction, mockListener));

        FxAssert.verifyThat("#itemState", LabeledMatchers.hasText("ENDED"));
    }

    /**
     * Khi AuctionStatus là SOLD, itemState phải hiển thị "SOLD".
     */
    @Test
    public void testSetData_StatusSold() {
        ItemDTO item = createItem(ItemType.VEHICLES, null);
        AuctionDTO auction = createAuction(item, AuctionStatus.SOLD, 9999, 9999);
        HandleCardClicked mockListener = mock(HandleCardClicked.class);

        interact(() -> controller.setData(auction, mockListener));

        FxAssert.verifyThat("#itemState", LabeledMatchers.hasText("SOLD"));
    }

    // =========================================================================
    // 3. testTypeSpecificLabel — kiểm tra label thuộc tính đặc thù của loại item
    // =========================================================================

    /**
     * Khi item có typeSpecificAttribute (ví dụ brand xe), typeSpecificLabel phải
     * hiển thị đúng nhãn "Brand: Toyota" và có visible=true, managed=true.
     */
    @Test
    public void testSetData_TypeSpecificLabel_Visible() {
        ItemDTO item = createItem(ItemType.VEHICLES, "Toyota");
        AuctionDTO auction = createAuction(item, AuctionStatus.ACTIVE, 1000, 5000);
        HandleCardClicked mockListener = mock(HandleCardClicked.class);

        interact(() -> controller.setData(auction, mockListener));

        Label typeSpecificLabel = lookup("#typeSpecificLabel").queryAs(Label.class);
        assertTrue(typeSpecificLabel.isVisible());
        assertTrue(typeSpecificLabel.isManaged());
        assertEquals("Brand: Toyota", typeSpecificLabel.getText());
    }

    /**
     * Khi item có typeSpecificAttribute là null, typeSpecificLabel phải ẩn đi.
     */
    @Test
    public void testSetData_TypeSpecificLabel_Hidden_WhenNull() {
        ItemDTO item = createItem(ItemType.ELECTRONICS, null);
        AuctionDTO auction = createAuction(item, AuctionStatus.ACTIVE, 500, 2000);
        HandleCardClicked mockListener = mock(HandleCardClicked.class);

        interact(() -> controller.setData(auction, mockListener));

        FxAssert.verifyThat("#typeSpecificLabel", NodeMatchers.isInvisible());
        Label typeSpecificLabel = lookup("#typeSpecificLabel").queryAs(Label.class);
        assertFalse(typeSpecificLabel.isManaged());
    }

    /**
     * Khi item có typeSpecificAttribute là blank (khoảng trắng), typeSpecificLabel phải ẩn đi.
     */
    @Test
    public void testSetData_TypeSpecificLabel_Hidden_WhenBlank() {
        ItemDTO item = createItem(ItemType.ARTS, "   ");
        AuctionDTO auction = createAuction(item, AuctionStatus.ACTIVE, 200, 1000);
        HandleCardClicked mockListener = mock(HandleCardClicked.class);

        interact(() -> controller.setData(auction, mockListener));

        FxAssert.verifyThat("#typeSpecificLabel", NodeMatchers.isInvisible());
        Label typeSpecificLabel = lookup("#typeSpecificLabel").queryAs(Label.class);
        assertFalse(typeSpecificLabel.isManaged());
    }

    /**
     * Kiểm tra nhãn đúng theo từng loại ItemType:
     * ARTS -> "Artist", ELECTRONICS -> "Model".
     */
    @Test
    public void testSetData_TypeAttributeLabel_ByType() {
        // ARTS -> "Artist"
        ItemDTO artsItem = createItem(ItemType.ARTS, "Picasso");
        AuctionDTO artsAuction = createAuction(artsItem, AuctionStatus.ACTIVE, 100, 500);
        HandleCardClicked mockListener = mock(HandleCardClicked.class);

        interact(() -> controller.setData(artsAuction, mockListener));

        Label typeSpecificLabel = lookup("#typeSpecificLabel").queryAs(Label.class);
        assertEquals("Artist: Picasso", typeSpecificLabel.getText());

        // ELECTRONICS -> "Model"
        ItemDTO elecItem = createItem(ItemType.ELECTRONICS, "RTX 4090");
        AuctionDTO elecAuction = createAuction(elecItem, AuctionStatus.ACTIVE, 800, 3000);

        interact(() -> controller.setData(elecAuction, mockListener));
        assertEquals("Model: RTX 4090", typeSpecificLabel.getText());
    }

    // =========================================================================
    // 4. testHandleCardClick — kiểm tra sự kiện click mở chi tiết đấu giá
    // =========================================================================

    /**
     * Khi handleCardClick() được gọi, listener phải được kích hoạt
     * với đúng AuctionDTO đã được set.
     */
    @Test
    public void testHandleCardClick_CallsListener() {
        ItemDTO item = createItem(ItemType.ELECTRONICS, null);
        AuctionDTO auction = createAuction(item, AuctionStatus.ACTIVE, 500, 2000);
        HandleCardClicked mockListener = mock(HandleCardClicked.class);

        interact(() -> controller.setData(auction, mockListener));
        interact(() -> controller.handleCardClick());

        verify(mockListener, times(1)).openAuctionDetail(auction);
    }

    /**
     * Mỗi lần click, listener chỉ được gọi đúng 1 lần.
     */
    @Test
    public void testHandleCardClick_CalledOnce() {
        ItemDTO item = createItem(ItemType.VEHICLES, "BMW");
        AuctionDTO auction = createAuction(item, AuctionStatus.ACTIVE, 5000, 20000);
        HandleCardClicked mockListener = mock(HandleCardClicked.class);

        interact(() -> controller.setData(auction, mockListener));
        interact(() -> controller.handleCardClick());
        interact(() -> controller.handleCardClick());

        verify(mockListener, times(2)).openAuctionDetail(auction);
    }

    // =========================================================================
    // 5. testBuyOut — kiểm tra gửi yêu cầu mua ngay (BUY_OUT)
    // =========================================================================

    /**
     * Khi buyOut() được gọi, controller phải gửi NetworkMessage với action "BUY_OUT"
     * và data là BidTransactionDTO chứa đúng giá mua và người dùng hiện tại.
     */
    @Test
    public void testBuyOut_SendsCorrectMessage() {
        UserSession.getInstance().setUser(testUser);

        ItemDTO item = createItem(ItemType.ELECTRONICS, "Dell XPS");
        AuctionDTO auction = createAuction(item, AuctionStatus.ACTIVE, 500, 2000);
        HandleCardClicked mockListener = mock(HandleCardClicked.class);

        interact(() -> controller.setData(auction, mockListener));
        interact(() -> controller.buyOut());

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService, times(1)).sendMessage(captor.capture());

        NetworkMessage sentMsg = captor.getValue();
        assertEquals("BUY_OUT", sentMsg.getAction());

        BidTransactionDTO transaction = (BidTransactionDTO) sentMsg.getData();
        assertEquals(2000.0, transaction.getBidAmount());
        assertEquals(testUser.getId(), transaction.getBidder().getId());
    }

    /**
     * buyOut() phải gắn đúng AuctionDTO vào BidTransactionDTO trong message.
     */
    @Test
    public void testBuyOut_TransactionContainsCorrectAuction() {
        UserSession.getInstance().setUser(testUser);

        ItemDTO item = createItem(ItemType.ARTS, "Monet");
        AuctionDTO auction = createAuction(item, AuctionStatus.ACTIVE, 300, 9999);
        HandleCardClicked mockListener = mock(HandleCardClicked.class);

        interact(() -> controller.setData(auction, mockListener));
        interact(() -> controller.buyOut());

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService).sendMessage(captor.capture());

        BidTransactionDTO transaction = (BidTransactionDTO) captor.getValue().getData();
        assertEquals(auction, transaction.getAuction());
        assertEquals(9999.0, transaction.getBidAmount());
    }

    // =========================================================================
    // 6. testFormatPrice — kiểm tra định dạng giá hiển thị
    // =========================================================================

    /**
     * Giá nguyên (không phần thập phân) phải hiển thị với dấu phân cách hàng nghìn,
     * không có phần ".00". Ví dụ: 1000 -> "1,000".
     */
    @Test
    public void testSetData_PriceFormat_Integer() {
        ItemDTO item = createItem(ItemType.ELECTRONICS, null);
        AuctionDTO auction = createAuction(item, AuctionStatus.ACTIVE, 1000, 5000);
        HandleCardClicked mockListener = mock(HandleCardClicked.class);

        interact(() -> controller.setData(auction, mockListener));

        FxAssert.verifyThat("#currentPrice", LabeledMatchers.hasText("1,000"));
        FxAssert.verifyThat("#buyOutPrice", LabeledMatchers.hasText("5,000"));
    }

    /**
     * Giá có phần thập phân phải hiển thị với 2 chữ số sau dấu phẩy.
     * Ví dụ: 1234.5 -> "1,234.50".
     */
    @Test
    public void testSetData_PriceFormat_Decimal() {
        ItemDTO item = createItem(ItemType.ARTS, null);
        AuctionDTO auction = createAuction(item, AuctionStatus.ACTIVE, 1234.5, 9999.99);
        HandleCardClicked mockListener = mock(HandleCardClicked.class);

        interact(() -> controller.setData(auction, mockListener));

        FxAssert.verifyThat("#currentPrice", LabeledMatchers.hasText("1,234.50"));
        FxAssert.verifyThat("#buyOutPrice", LabeledMatchers.hasText("9,999.99"));
    }
}
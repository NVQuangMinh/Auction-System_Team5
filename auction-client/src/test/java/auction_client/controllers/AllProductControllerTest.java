package auction_client.controllers;

import auction_client.Network.ClientService;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.AuctionDTO;
import auction_shared.dto.AuctionStatus;
import auction_shared.dto.ItemDTO;
import auction_shared.dto.ItemType;
import auction_shared.dto.ProductListResponse;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.testfx.framework.junit5.ApplicationTest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test cho AllProductController.
 *
 * ============================================================
 * CÁC LỖI COMPILE ĐÃ SỬA
 * ============================================================
 *
 * LỖI 1 – SAI TÊN FXML:
 *   Cũ: getResource("/auction_client/AllProduct.fxml")  → URL = null → NPE
 *   Sửa: "AllProductScene.fxml" (tên file thực tế trong resources)
 *
 * LỖI 2 – MOCK CHƯA SẴN SÀNG KHI FXML NẠP:
 *   start(Stage) chạy TRƯỚC @BeforeEach → initialize() thấy ClientService thật.
 *   Sửa: tạo MockedStatic BÊN TRONG start(), TRƯỚC loader.load().
 *
 * LỖI 3 – ProductListResponse KHÔNG CÓ no-arg constructor & KHÔNG CÓ setter:
 *   Cũ: new ProductListResponse() + response.setActiveAuctions(...) → không compile.
 *   Sửa: dùng constructor 4 tham số:
 *     new ProductListResponse(activeList, endedList, endedTotalCount, activeTotalCount)
 *
 * LỖI 4 – onStatusSelected() là private:
 *   Cũ: controller.onStatusSelected() gọi trực tiếp → không compile (private method).
 *   Sửa: Kích hoạt qua giao diện thật — dùng clickOn("#endedStatusRadio") để
 *     TestFX click RadioButton, JavaFX tự kích hoạt onAction="#onStatusSelected".
 * ============================================================
 */
public class AllProductControllerTest extends ApplicationTest {
    private AllProductController controller;
    private ClientService mockClientService;
    private MockedStatic<ClientService> mockedStaticClientService;

    @AfterEach
    public void closeMock() {
        if (mockedStaticClientService != null) {
            mockedStaticClientService.close();
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        // ① Mock TRƯỚC khi load FXML – initialize() sẽ thấy mock ngay
        mockClientService = mock(ClientService.class);
        mockedStaticClientService = mockStatic(ClientService.class);
        mockedStaticClientService.when(() -> ClientService.getInstance()).thenReturn(mockClientService);

        // ② Đúng tên file FXML
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/auction_client/AllProductScene.fxml")
        );
        Parent root = loader.load();
        controller = loader.getController();

        stage.setScene(new Scene(root));
        stage.show();
    }

    // ------------------------------------------------------------------
    // TEST 1: initialize() phải đăng ký listener và gửi GET_PRODUCTS
    // ------------------------------------------------------------------
    @Test
    public void testInitialize_SendsGetProductsMessage() {
        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);

        verify(mockClientService).addListener(controller);
        verify(mockClientService).sendMessage(captor.capture());

        assertEquals("GET_PRODUCTS", captor.getValue().getAction());
    }

    // ------------------------------------------------------------------
    // TEST 2: Click RadioButton "Ended" → phải gửi GET_ENDED_PRODUCTS
    //
    // LỖI CŨ: controller.onStatusSelected() — private method, không compile.
    // SỬA: clickOn("#endedStatusRadio") → TestFX click thật lên RadioButton,
    //   JavaFX tự kích hoạt onAction="#onStatusSelected" khai báo trong FXML.
    // ------------------------------------------------------------------
    @Test
    public void testSwitchToEndedStatus_SendsGetEndedProductsMessage() {
        // Click trực tiếp lên RadioButton trong giao diện
        clickOn("#endedStatusRadio");

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        // atLeastOnce() vì sendMessage đã được gọi 1 lần trong initialize()
        verify(mockClientService, atLeastOnce()).sendMessage(captor.capture());

        // Lấy tin nhắn cuối cùng
        List<NetworkMessage> allMessages = captor.getAllValues();
        String lastAction = allMessages.get(allMessages.size() - 1).getAction();
        assertEquals("GET_ENDED_PRODUCTS", lastAction);
    }

    // ------------------------------------------------------------------
    // TEST 3: Nhận GET_PRODUCTS response → FlowPane tồn tại
    //
    // LỖI CŨ:
    //   new ProductListResponse()                 → không có no-arg constructor
    //   response.setActiveAuctions(...)           → không có setter
    //   new ItemDTO() + item.setType(...)         → không có no-arg + setter
    //   new AuctionDTO() + auction.setId(...)     → không có no-arg + setter
    //
    // SỬA: Dùng đúng constructor tham số của từng class.
    // ------------------------------------------------------------------
    @Test
    public void testOnUpdateReceived_ActiveProducts_PopulatesFlowPane() {
        // ① ItemDTO(String id, String itemName, String description, UserDTO owner, ItemType type)
        ItemDTO item = new ItemDTO(
                "item-001",
                "Test Artwork",
                "A test item",
                null,           // owner – không cần thiết cho test này
                ItemType.ARTS
        );

        // ② AuctionDTO(ItemDTO, AuctionStatus, double startingPrice, double buyOutPrice,
        //              double tickSize, LocalDateTime startTime, LocalDateTime endTime,
        //              boolean antiSniping, String winnerId, double currentHighestBid)
        AuctionDTO activeAuction = new AuctionDTO(
                item,
                AuctionStatus.ACTIVE,
                100.0,
                500.0,
                10.0,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                false,
                null,
                100.0
        );

        // ③ ProductListResponse(List<AuctionDTO> active, List<AuctionDTO> ended,
        //                       int endedTotalCount, int activeTotalCount)
        ProductListResponse response = new ProductListResponse(
                Collections.singletonList(activeAuction),  // activeAuctions
                Collections.emptyList(),                   // endedSaledAuctions
                0,                                         // endedTotalCount
                1                                          // activeTotalCount
        );

        NetworkMessage msg = new NetworkMessage("GET_PRODUCTS", response);

        // Đẩy message vào controller trên JavaFX thread
        interact(() -> controller.onUpdateReceived(msg));

        // FlowPane phải tồn tại
        FlowPane flowPane = lookup("#productFlowPane").queryAs(FlowPane.class);
        assertNotNull(flowPane, "productFlowPane không được null");
    }

    // ------------------------------------------------------------------
    // TEST 4: pageInfoLabel mặc định phải là "1 / 1" (trạng thái ACTIVE)
    // ------------------------------------------------------------------
    @Test
    public void testPageInfoLabel_ShowsDefaultPage() {
        Label pageInfo = lookup("#pageInfoLabel").queryAs(Label.class);
        assertNotNull(pageInfo);
        assertEquals("1 / 1", pageInfo.getText());
    }

    // ------------------------------------------------------------------
    // TEST 5: cleanup() phải gỡ listener khỏi ClientService
    // ------------------------------------------------------------------
    @Test
    public void testCleanup_RemovesListener() {
        interact(() -> controller.cleanup());
        verify(mockClientService).removeListener(controller);
    }

    // ------------------------------------------------------------------
    // TEST 6: Nhận AUCTION_ENDED → phải gọi showNotification
    // ------------------------------------------------------------------
    @Test
    public void testOnUpdateReceived_AuctionEnded_Notification() {
        try (MockedStatic<UserPushUpNotificationController> mockedNotification = mockStatic(UserPushUpNotificationController.class)) {
            ItemDTO item = new ItemDTO("item-1", "Laptop", "desc", null, ItemType.ELECTRONICS);
            AuctionDTO dto = new AuctionDTO(
                    item,
                    AuctionStatus.ENDED,
                    100.0,
                    500.0,
                    10.0,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    false,
                    null,
                    100.0
            );

            NetworkMessage msg = new NetworkMessage("AUCTION_ENDED", dto);

            interact(() -> controller.onUpdateReceived(msg));

            mockedNotification.verify(() -> UserPushUpNotificationController.showNotification(
                    "Phiên đấu giá kết thúc: Laptop", "INFO"
            ), times(1));
        }
    }

    // ------------------------------------------------------------------
    // TEST 7: Click nút LÀM MỚI (Refresh)
    // ------------------------------------------------------------------
    @Test
    public void testRefreshButton_Click() {
        // Mặc định đang ở tab Active
        clickOn("#refreshButton");

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService, atLeastOnce()).sendMessage(captor.capture());

        List<NetworkMessage> allMessages = captor.getAllValues();
        String lastAction = allMessages.get(allMessages.size() - 1).getAction();
        assertEquals("GET_PRODUCTS", lastAction);
    }
}
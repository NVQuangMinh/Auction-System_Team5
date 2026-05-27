package auction_client.controllers.seller;

import auction_client.Network.ClientService;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.matcher.control.LabeledMatchers;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SellProductInfoControllerTest extends ApplicationTest {

    private SellProductInfoController controller;
    private ClientService mockClientService;
    private MockedStatic<ClientService> mockedStaticClientService;

    private final UserDTO testUser = new UserDTO("67", "nhan", "USER");
    private final ItemDTO testItem = new ItemDTO("01", "May tinh", "Mot cai may tinh", testUser, ItemType.VEHICLES);

    private AuctionDTO createTestAuction(AuctionStatus status) {
        return new AuctionDTO(testItem, status, 0, 9999, 10,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusSeconds(150),
                false, null, 100);
    }

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

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/SellProductInfo.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    public void testInitialize() {
        assertFalse(controller.bidHistory.getData().isEmpty());
    }

    @Test
    public void testInitData_ActiveAuction() {
        AuctionDTO auction = createTestAuction(AuctionStatus.ACTIVE);
        interact(() -> controller.initData(auction));

        verify(mockClientService).addListener(controller);

        FxAssert.verifyThat("#itemName", LabeledMatchers.hasText("May tinh"));
        FxAssert.verifyThat("#description", LabeledMatchers.hasText("Mot cai may tinh"));
        FxAssert.verifyThat("#currentPrice", LabeledMatchers.hasText("100.0"));
        FxAssert.verifyThat("#buyOut", LabeledMatchers.hasText("9999.0"));
        FxAssert.verifyThat("#tickRate", LabeledMatchers.hasText("10.0"));

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService).sendMessage(captor.capture());
        assertEquals("GET_BID_HISTORY", captor.getValue().getAction());
        assertEquals(auction, captor.getValue().getData());
    }

    @Test
    public void testInitData_EndedAuction() {
        AuctionDTO auction = createTestAuction(AuctionStatus.ENDED);
        interact(() -> controller.initData(auction));

        FxAssert.verifyThat("#timeLeft", LabeledMatchers.hasText("ENDED"));

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService).sendMessage(captor.capture());
        assertEquals("GET_BID_HISTORY", captor.getValue().getAction());
        assertEquals(auction, captor.getValue().getData());
    }

    @Test
    public void testInitData_SoldAuction() {
        AuctionDTO auction = createTestAuction(AuctionStatus.SOLD);
        interact(() -> controller.initData(auction));

        FxAssert.verifyThat("#timeLeft", LabeledMatchers.hasText("SOLD"));

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService).sendMessage(captor.capture());
        assertEquals("GET_BID_HISTORY", captor.getValue().getAction());
    }

    @Test
    public void testInitData_BannedAuction() {
        AuctionDTO auction = createTestAuction(AuctionStatus.BANNED);
        interact(() -> controller.initData(auction));

        FxAssert.verifyThat("#timeLeft", LabeledMatchers.hasText("BANNED"));

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService).sendMessage(captor.capture());
        assertEquals("GET_BID_HISTORY", captor.getValue().getAction());
    }

    @Test
    public void testOnUpdateReceived_GetBidHistory() {
        AuctionDTO auction = createTestAuction(AuctionStatus.ACTIVE);
        interact(() -> controller.initData(auction));

        BidTransactionDTO t1 = new BidTransactionDTO(auction, testUser, 110.0, LocalDateTime.now().plusHours(9));
        BidTransactionDTO t2 = new BidTransactionDTO(auction, testUser, 120.0, LocalDateTime.now().plusHours(10));
        List<BidTransactionDTO> transactions = List.of(t1, t2);
        NetworkMessage msg = new NetworkMessage("GET_BID_HISTORY", (Serializable) transactions);
        controller.onUpdateReceived(msg);
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

        LineChart<String, Number> chart = lookup("#bidHistory").queryAs(LineChart.class);
        XYChart.Series<String, Number> priceSeries = chart.getData().get(0);

        assertEquals(2, priceSeries.getData().size());
        assertEquals(t1.getBidTime().toString(), priceSeries.getData().get(0).getXValue());
        assertEquals(110.0, priceSeries.getData().get(0).getYValue());
        assertEquals(t2.getBidTime().toString(), priceSeries.getData().get(1).getXValue());
        assertEquals(120.0, priceSeries.getData().get(1).getYValue());
    }

    @Test
    public void testOnUpdateReceived_UpdateBid() {
        AuctionDTO auction = createTestAuction(AuctionStatus.ACTIVE);
        interact(() -> controller.initData(auction));

        AuctionDTO updatedAuction = new AuctionDTO(testItem,
                AuctionStatus.ACTIVE,
                0,
                9999,
                10,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusSeconds(150),
                false,
                null,
                200);
        List<AuctionDTO> allRooms = List.of(updatedAuction);
        NetworkMessage msg = new NetworkMessage("UPDATE_BID", (Serializable) allRooms);
        controller.onUpdateReceived(msg);
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

        FxAssert.verifyThat("#currentPrice", LabeledMatchers.hasText("200.0"));

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService, times(2)).sendMessage(captor.capture());
        assertEquals("GET_BID_HISTORY", captor.getAllValues().get(1).getAction());
        assertEquals(updatedAuction, captor.getAllValues().get(1).getData());
    }

    @Test
    public void testOnUpdateReceived_UpdateBid_Sold() {
        AuctionDTO auction = createTestAuction(AuctionStatus.ACTIVE);
        interact(() -> controller.initData(auction));

        AuctionDTO soldAuction = createTestAuction(AuctionStatus.SOLD);
        List<AuctionDTO> allRooms = List.of(soldAuction);
        NetworkMessage msg = new NetworkMessage("UPDATE_BID", (Serializable) allRooms);
        controller.onUpdateReceived(msg);
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

        FxAssert.verifyThat("#timeLeft", LabeledMatchers.hasText("SOLD"));
    }

    @Test
    public void testCleanUp() {
        AuctionDTO auction = createTestAuction(AuctionStatus.ACTIVE);
        interact(() -> controller.initData(auction));

        interact(() -> controller.cleanup());

        verify(mockClientService).removeListener(controller);
    }
}

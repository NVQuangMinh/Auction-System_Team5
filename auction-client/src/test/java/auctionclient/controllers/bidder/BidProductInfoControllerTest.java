package auctionclient.controllers.bidder;

import auctionclient.Network.ClientService;
import auctionclient.UserSession;
import auctionshared.Network.NetworkMessage;
import auctionshared.dto.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.matcher.control.LabeledMatchers;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BidProductInfoControllerTest extends ApplicationTest {
    private BidProductInfoController controller;
    private ClientService mockClientService;

    private UserDTO testUser1 = new UserDTO("67", "nhan", "USER");
    private UserDTO testUser2 = new UserDTO("82", "nam", "USER");
    private ItemDTO testItem = new ItemDTO("01", "May tinh", "Mot cai may tinh", testUser1, ItemType.VEHICLES);
    public AuctionDTO createTestAuction(AuctionStatus status) {
        return new AuctionDTO(testItem, status, 0, 9999, 10, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusSeconds(150), false, null, 100);
    }


    @AfterEach
    public void closeMock() {
        try {
            java.lang.reflect.Field instanceField = ClientService.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        mockClientService = mock(ClientService.class);
        java.lang.reflect.Field instanceField = ClientService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, mockClientService);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/auctionclient/BidProductInfo.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    public void testInitialize() {
        verify(mockClientService).addListener(controller);

        Label error = lookup("#error").queryAs(Label.class);
        assertNotNull(error);
        FxAssert.verifyThat("#error", NodeMatchers.isInvisible());
        assertFalse(error.isManaged());

        assertFalse(controller.bidHistory.getData().isEmpty());
    }

    @Test
    public void testUpdateData(){
        AuctionDTO auction = createTestAuction(AuctionStatus.ACTIVE);

        interact(() -> controller.initData(auction));

        FxAssert.verifyThat("#itemName", LabeledMatchers.hasText("May tinh"));
        FxAssert.verifyThat("#description", LabeledMatchers.hasText("Mot cai may tinh"));
        FxAssert.verifyThat("#currentPrice", LabeledMatchers.hasText("$100"));
        FxAssert.verifyThat("#buyOut", LabeledMatchers.hasText("$9,999"));
        FxAssert.verifyThat("#tickRate", LabeledMatchers.hasText("$10"));

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
        assertEquals(auction, captor.getValue().getData());
    }
    @Test
    public void testInitData_BannedAuction() {
        AuctionDTO auction = createTestAuction(AuctionStatus.BANNED);
        interact(() -> controller.initData(auction));

        FxAssert.verifyThat("#timeLeft", LabeledMatchers.hasText("BANNED"));

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService).sendMessage(captor.capture());
        assertEquals("GET_BID_HISTORY", captor.getValue().getAction());
        assertEquals(auction, captor.getValue().getData());
    }

    @Test
    void testOnUpdateReceived_BidSuccess() {
        AuctionDTO auction = createTestAuction(AuctionStatus.ACTIVE);
        interact(() -> controller.initData(auction));
        AuctionDTO updatedAuction = new AuctionDTO(testItem,
            AuctionStatus.ACTIVE,
            0,
            9999,
            1,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusSeconds(150),
            false,
            null,
            //gia lap bid them 100
            100 + 100 );
        NetworkMessage msg = new NetworkMessage("BID_SUCCESS", updatedAuction);
        controller.onUpdateReceived(msg);
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
        FxAssert.verifyThat("#currentPrice", LabeledMatchers.hasText("$200"));
        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService, times(2)).sendMessage(captor.capture());
        assertEquals("GET_BID_HISTORY", captor.getAllValues().get(1).getAction());
    }

    @Test
    void testOnUpdateReceived_BuyOutSuccess() {
        AuctionDTO auction = createTestAuction(AuctionStatus.ACTIVE);
        interact(() -> controller.initData(auction));
        AuctionDTO soldAuction = createTestAuction(AuctionStatus.SOLD);
        NetworkMessage msg = new NetworkMessage("BUYOUT_SUCCESS", soldAuction);
        controller.onUpdateReceived(msg);
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

        FxAssert.verifyThat("#timeLeft", LabeledMatchers.hasText("SOLD"));
        TextField bidAmountField = lookup("#bidAmount").queryAs(TextField.class);
        assertFalse(bidAmountField.isEditable());

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService, times(1)).sendMessage(captor.capture());
    }

    @Test
    public void testOnUpdateReceived_GetBidHistory() {
        AuctionDTO auction = createTestAuction(AuctionStatus.ACTIVE);
        interact(() -> controller.initData(auction));

        BidTransactionDTO t1 = new BidTransactionDTO(auction, testUser1, 110.0, LocalDateTime.now().plusHours(9));
        BidTransactionDTO t2 = new BidTransactionDTO(auction, testUser2, 120.0, LocalDateTime.now().plusHours(10));
        List<BidTransactionDTO> transactions = List.of(t1, t2);
        NetworkMessage msg = new NetworkMessage("GET_BID_HISTORY",(Serializable) transactions);
        controller.onUpdateReceived(msg);
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

        DateTimeFormatter formater = DateTimeFormatter.ofPattern("HH:mm");
        LineChart<String, Number> chart = lookup("#bidHistory").queryAs(LineChart.class);

        XYChart.Series<String, Number> priceSeries1 = chart.getData().get(0);
        assertEquals(2, priceSeries1.getData().size());
        assertEquals(t1.getBidTime().format(formater), priceSeries1.getData().get(0).getXValue());
        assertEquals(110.0, priceSeries1.getData().get(0).getYValue());

        XYChart.Series<String, Number> priceSeries2 = chart.getData().get(0);
        assertEquals(2, priceSeries2.getData().size());
        assertEquals(t2.getBidTime().format(formater), priceSeries2.getData().get(1).getXValue());
        assertEquals(120.0, priceSeries2.getData().get(1).getYValue());
    }

    @Test
    public void testPlaceBidRequest_ValidAmount() {
        UserSession.getInstance().setUser(testUser1);

        AuctionDTO auction = createTestAuction(AuctionStatus.ACTIVE);
        interact(() -> controller.initData(auction));

        TextField bidAmountField = lookup("#bidAmount").queryAs(TextField.class);
        interact(() -> bidAmountField.setText("120"));
        interact(() -> controller.placeBidRequest());

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService, times(2)).sendMessage(captor.capture());

        NetworkMessage sentMsg = captor.getAllValues().get(1);
        assertEquals("PLACE_BID", sentMsg.getAction());

        BidTransactionDTO transaction = (BidTransactionDTO) sentMsg.getData();
        assertEquals(120.0, transaction.getBidAmount());
        assertEquals(testUser1.getId(), transaction.getBidder().getId());

        FxAssert.verifyThat("#error", NodeMatchers.isInvisible());
    }

    @Test
    public void testPlaceBidRequest_InvalidAmount() {
        AuctionDTO auction = createTestAuction(AuctionStatus.ACTIVE); // Giá $100, bước giá $10
        interact(() -> controller.initData(auction));

        TextField bidAmountField = lookup("#bidAmount").queryAs(TextField.class);
        interact(() -> bidAmountField.setText("105"));

        interact(() -> controller.placeBidRequest());

        // Kiểm tra không gửi thêm tin nhắn nào sau initData
        verify(mockClientService, times(1)).sendMessage(any());

        // Nhãn error phải hiển thị "Invalid Bid Amount!" màu đỏ
        Label errorLabel = lookup("#error").queryAs(Label.class);
        assertTrue(errorLabel.isVisible());
        assertTrue(errorLabel.isManaged());
        assertEquals("Invalid Bid Amount!", errorLabel.getText());
        assertEquals(Color.RED, errorLabel.getTextFill());
    }

    @Test
    public void testPlaceBidRequest_BuyOut() {
        AuctionDTO auction = createTestAuction(AuctionStatus.ACTIVE); // Giá mua đứt $9999
        interact(() -> controller.initData(auction));

        TextField bidAmountField = lookup("#bidAmount").queryAs(TextField.class);
        interact(() -> bidAmountField.setText("9999")); // Đạt mức mua đứt

        interact(() -> controller.placeBidRequest());

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService, times(2)).sendMessage(captor.capture());

        NetworkMessage sentMsg = captor.getAllValues().get(1);
        assertEquals("BUY_OUT", sentMsg.getAction());
    }

    @Test
    void testPlaceBidRequest_NonNumericAmount() {
        AuctionDTO auction = createTestAuction(AuctionStatus.ACTIVE);
        interact(() -> controller.initData(auction));

        TextField bidAmountField = lookup("#bidAmount").queryAs(TextField.class);
        interact(() -> bidAmountField.setText("abc"));
        interact(() -> controller.placeBidRequest());

        verify(mockClientService, times(1)).sendMessage(any());

        // Hiển thị thông báo "Please enter a valid number!"
        FxAssert.verifyThat("#error", NodeMatchers.isVisible());
        FxAssert.verifyThat("#error", LabeledMatchers.hasText("Please enter a valid number!"));
    }

}
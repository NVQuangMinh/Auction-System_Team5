package auctionclient.controllers.seller;

import auctionclient.Network.ClientService;
import auctionshared.Network.NetworkMessage;
import auctionshared.dto.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.testfx.api.FxAssert;
import auctionclient.controllers.FxControllerTestBase;
import org.testfx.matcher.control.LabeledMatchers;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SellProductInfoControllerTest extends FxControllerTestBase {

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

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/auctionclient/SellProductInfo.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    public void testInitData_EndedAuction() {
        AuctionDTO auction = createTestAuction(AuctionStatus.ENDED);
        interact(() -> controller.initData(auction));

        FxAssert.verifyThat("#timeLeft", LabeledMatchers.hasText("ENDED"));
        FxAssert.verifyThat("#itemName", LabeledMatchers.hasText("May tinh"));
        FxAssert.verifyThat("#description", LabeledMatchers.hasText("Mot cai may tinh"));
        FxAssert.verifyThat("#currentPrice", LabeledMatchers.hasText("$100"));
        FxAssert.verifyThat("#buyOut", LabeledMatchers.hasText("$9,999"));
        FxAssert.verifyThat("#tickRate", LabeledMatchers.hasText("$10"));
    }

    @Test
    public void testInitData_SoldAuction() {
        AuctionDTO auction = createTestAuction(AuctionStatus.SOLD);
        interact(() -> controller.initData(auction));

        FxAssert.verifyThat("#timeLeft", LabeledMatchers.hasText("SOLD"));
        FxAssert.verifyThat("#itemName", LabeledMatchers.hasText("May tinh"));
        FxAssert.verifyThat("#description", LabeledMatchers.hasText("Mot cai may tinh"));
        FxAssert.verifyThat("#currentPrice", LabeledMatchers.hasText("$100"));
        FxAssert.verifyThat("#buyOut", LabeledMatchers.hasText("$9,999"));
        FxAssert.verifyThat("#tickRate", LabeledMatchers.hasText("$10"));
    }

    @Test
    public void testInitData_BannedAuction() {
        AuctionDTO auction = createTestAuction(AuctionStatus.BANNED);
        interact(() -> controller.initData(auction));

        FxAssert.verifyThat("#timeLeft", LabeledMatchers.hasText("BANNED"));
        FxAssert.verifyThat("#itemName", LabeledMatchers.hasText("May tinh"));
        FxAssert.verifyThat("#description", LabeledMatchers.hasText("Mot cai may tinh"));
        FxAssert.verifyThat("#currentPrice", LabeledMatchers.hasText("$100"));
        FxAssert.verifyThat("#buyOut", LabeledMatchers.hasText("$9,999"));
        FxAssert.verifyThat("#tickRate", LabeledMatchers.hasText("$10"));
    }

    @Test
    public void testOnUpdateReceived_GetBidHistory_AddsCorrectPricesToLineChart() {
        AuctionDTO auction = createTestAuction(AuctionStatus.ENDED);
        interact(() -> controller.initData(auction));

        BidTransactionDTO t1 = new BidTransactionDTO(auction, testUser, 110.0, LocalDateTime.now().plusHours(9));
        BidTransactionDTO t2 = new BidTransactionDTO(auction, testUser, 120.0, LocalDateTime.now().plusHours(10));
        List<BidTransactionDTO> transactions = List.of(t1, t2);

        NetworkMessage msg = new NetworkMessage("GET_BID_HISTORY", (Serializable) transactions);
        controller.onUpdateReceived(msg);
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

        LineChart<?, ?> chart = lookup("#bidHistory").queryAs(LineChart.class);
        XYChart.Series<?, ?> priceSeries = chart.getData().get(0);

        assertEquals(2, priceSeries.getData().size());
        assertEquals(110.0, ((Number) priceSeries.getData().get(0).getYValue()).doubleValue());
        assertEquals(120.0, ((Number) priceSeries.getData().get(1).getYValue()).doubleValue());
    }
}

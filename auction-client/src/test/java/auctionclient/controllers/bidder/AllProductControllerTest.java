package auctionclient.controllers.bidder;

import auctionclient.Network.ClientService;
import auctionshared.Network.NetworkMessage;
import auctionshared.dto.AuctionDTO;
import auctionshared.dto.AuctionStatus;
import auctionshared.dto.ItemDTO;
import auctionshared.dto.ItemType;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Kiểm tra lọc sản phẩm theo trạng thái (Active / Ended) và danh mục
 * (All, Arts, Vehicles, Electronics) — 8 trường hợp.
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
        mockClientService = mock(ClientService.class);
        mockedStaticClientService = mockStatic(ClientService.class);
        mockedStaticClientService.when(ClientService::getInstance).thenReturn(mockClientService);

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/auctionclient/AllProductScene.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        stage.setScene(new Scene(root));
        stage.show();
    }

    @BeforeEach
    public void resetToActiveAll() {
        clearInvocations(mockClientService);
        pushActiveProducts(sampleAuctions(AuctionStatus.ACTIVE));
        interact(() -> {
            lookup("#activeStatusRadio").queryAs(RadioButton.class).setSelected(true);
            lookup("#allCategoryRadio").queryAs(RadioButton.class).setSelected(true);
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    // --- Active: All, Arts, Vehicles, Electronics ---

    @Test
    public void testActiveCategory_All_ShowsAllProducts() {
        assertEquals(3, flowPaneChildCount());
    }

    @Test
    public void testActiveCategory_Arts_ShowsOnlyArts() {
        clickOn("#artsCategoryRadio");
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(1, flowPaneChildCount());
    }

    @Test
    public void testActiveCategory_Vehicles_ShowsOnlyVehicles() {
        clickOn("#vehiclesCategoryRadio");
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(1, flowPaneChildCount());
    }

    @Test
    public void testActiveCategory_Electronics_ShowsOnlyElectronics() {
        clickOn("#electronicsCategoryRadio");
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(1, flowPaneChildCount());
    }

    // --- Ended: All, Arts, Vehicles, Electronics ---

    @Test
    public void testEndedCategory_All_ShowsAllProducts() {
        clickOn("#endedStatusRadio");
        pushEndedProducts(sampleAuctions(AuctionStatus.ENDED));
        assertEquals(3, flowPaneChildCount());
    }

    @Test
    public void testEndedCategory_Arts_ShowsOnlyArts() {
        clickOn("#endedStatusRadio");
        clickOn("#artsCategoryRadio");
        pushEndedProducts(List.of(createAuction("art-1", "Painting", ItemType.ARTS, AuctionStatus.ENDED)));
        assertEquals(1, flowPaneChildCount());
    }

    @Test
    public void testEndedCategory_Vehicles_ShowsOnlyVehicles() {
        clickOn("#endedStatusRadio");
        clickOn("#vehiclesCategoryRadio");
        pushEndedProducts(List.of(createAuction("veh-1", "Car", ItemType.VEHICLES, AuctionStatus.ENDED)));
        assertEquals(1, flowPaneChildCount());
    }

    @Test
    public void testEndedCategory_Electronics_ShowsOnlyElectronics() {
        clickOn("#endedStatusRadio");
        clickOn("#electronicsCategoryRadio");
        pushEndedProducts(List.of(createAuction("elec-1", "Laptop", ItemType.ELECTRONICS, AuctionStatus.ENDED)));
        assertEquals(1, flowPaneChildCount());
    }

    private List<AuctionDTO> sampleAuctions(AuctionStatus status) {
        return List.of(
                createAuction("art-1", "Painting", ItemType.ARTS, status),
                createAuction("elec-1", "Laptop", ItemType.ELECTRONICS, status),
                createAuction("veh-1", "Car", ItemType.VEHICLES, status));
    }

    private AuctionDTO createAuction(String id, String name, ItemType type, AuctionStatus status) {
        ItemDTO item = new ItemDTO(id, name, "description", null, type);
        return new AuctionDTO(
                item,
                status,
                100.0,
                500.0,
                10.0,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                false,
                null,
                100.0);
    }

    private void pushActiveProducts(List<AuctionDTO> auctions) {
        controller.onUpdateReceived(
                new NetworkMessage("GET_ACTIVE_PRODUCTS", (Serializable) auctions));
        WaitForAsyncUtils.waitForFxEvents();
    }

    private void pushEndedProducts(List<AuctionDTO> auctions) {
        controller.onUpdateReceived(
                new NetworkMessage("GET_ENDED_PRODUCTS", (Serializable) auctions));
        WaitForAsyncUtils.waitForFxEvents();
    }

    private int flowPaneChildCount() {
        FlowPane flowPane = lookup("#productFlowPane").queryAs(FlowPane.class);
        return flowPane.getChildren().size();
    }
}

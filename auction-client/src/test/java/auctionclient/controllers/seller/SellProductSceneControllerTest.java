package auctionclient.controllers.seller;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.matcher.control.LabeledMatchers;
import org.testfx.util.WaitForAsyncUtils;

import auctionclient.Network.ClientService;
import auctionclient.UserSession;
import auctionshared.Network.NetworkMessage;
import auctionshared.dto.AuctionDTO;
import auctionshared.dto.AuctionStatus;
import auctionshared.dto.ItemDTO;
import auctionshared.dto.ItemType;
import auctionshared.dto.UserDTO;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.Label;
import javafx.stage.Stage;


public class SellProductSceneControllerTest extends ApplicationTest {

    private static final String TEST_USERNAME = "nhan";

    private SellProductSceneController controller;
    private ClientService mockClientService;
    private MockedStatic<ClientService> mockedStaticClientService;

    private final UserDTO testUser = new UserDTO("67", TEST_USERNAME, "USER");

    private AuctionDTO createAuction(String itemId, String itemName, ItemType itemType, String typeSpecificAttribute) {
        ItemDTO item = new ItemDTO(itemId, itemName, "Mo ta " + itemName, testUser, itemType, typeSpecificAttribute);
        return new AuctionDTO(
                item,
                AuctionStatus.ENDED,
                100.0,
                500.0,
                10.0,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                false,
                null,
                100.0
        );
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

        UserSession.getInstance().setUsername(TEST_USERNAME);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/auctionclient/SellProductScene.fxml")
        );
        Parent root = loader.load();
        controller = loader.getController();

        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    public void testAddArtsProduct_LoadsCorrectInfoOnCard() {
        assertCardLoadedCorrectly(
                createAuction("art-1", "Tranh Son Dau", ItemType.ARTS, "Van Gogh"),
                "Tranh Son Dau",
                "ENDED",
                "$100",
                "$500",
                "Mo ta Tranh Son Dau");
    }

    @Test
    public void testAddVehicleProduct_LoadsCorrectInfoOnCard() {
        assertCardLoadedCorrectly(
                createAuction("veh-1", "Xe Hoi", ItemType.VEHICLES, "Toyota"),
                "Xe Hoi",
                "ENDED",
                "$100",
                "$500",
                "Mo ta Xe Hoi");
    }

    @Test
    public void testAddElectronicProduct_LoadsCorrectInfoOnCard() {
        assertCardLoadedCorrectly(
                createAuction("ele-1", "Laptop", ItemType.ELECTRONICS, "MacBook Pro"),
                "Laptop",
                "ENDED",
                "$100",
                "$500",
                "Mo ta Laptop");
    }

    @Test
    public void testClickArtsProductCard_LoadsSellProductInfoPage() {
        assertCardClickOpensInfoPage(
                createAuction("art-1", "Tranh Son Dau", ItemType.ARTS, "Van Gogh"),
                "Artist: Van Gogh");
    }

    @Test
    public void testClickVehicleProductCard_LoadsSellProductInfoPage() {
        assertCardClickOpensInfoPage(
                createAuction("veh-1", "Xe Hoi", ItemType.VEHICLES, "Toyota"),
                "Brand: Toyota");
    }

    @Test
    public void testClickElectronicProductCard_LoadsSellProductInfoPage() {
        assertCardClickOpensInfoPage(
                createAuction("ele-1", "Laptop", ItemType.ELECTRONICS, "MacBook Pro"),
                "Model: MacBook Pro");
    }

    private Node pushAuctionAndGetFirstCard(AuctionDTO auction) {
        controller.onUpdateReceived(new NetworkMessage("GET_MY_LIST", (Serializable) List.of(auction)));
        WaitForAsyncUtils.waitForFxEvents();

        FlowPane flowPane = lookup("#myListFlowPane").queryAs(FlowPane.class);
        assertNotNull(flowPane);
        assertEquals(1, flowPane.getChildren().size());
        return flowPane.getChildren().get(0);
    }

    private void assertCardLoadedCorrectly(
            AuctionDTO auction,
            String expectedName,
            String expectedState,
            String expectedCurrentPrice,
            String expectedBuyOutPrice,
            String expectedDescription) {
        Node card = pushAuctionAndGetFirstCard(auction);

        Label itemName = (Label) card.lookup("#itemName");
        Label itemState = (Label) card.lookup("#itemState");
        Label currentPrice = (Label) card.lookup("#currentPrice");
        Label buyOutPrice = (Label) card.lookup("#buyOutPrice");
        Label description = (Label) card.lookup("#description");

        assertNotNull(itemName);
        assertNotNull(itemState);
        assertNotNull(currentPrice);
        assertNotNull(buyOutPrice);
        assertNotNull(description);

        assertEquals(expectedName, itemName.getText());
        assertEquals(expectedState, itemState.getText());
        assertEquals(expectedCurrentPrice, currentPrice.getText());
        assertEquals(expectedBuyOutPrice, buyOutPrice.getText());
        assertEquals(expectedDescription, description.getText());
    }

    private void assertCardClickOpensInfoPage(AuctionDTO auction, String expectedTypeSpecificText) {
        Node card = pushAuctionAndGetFirstCard(auction);
        clickOn(card);
        WaitForAsyncUtils.waitForFxEvents();

        FxAssert.verifyThat("#timeLeft", LabeledMatchers.hasText("ENDED"));
        FxAssert.verifyThat("#typeSpecificDisplay", LabeledMatchers.hasText(expectedTypeSpecificText));
        clickOn("#closeButton");
        WaitForAsyncUtils.waitForFxEvents();
    }
}

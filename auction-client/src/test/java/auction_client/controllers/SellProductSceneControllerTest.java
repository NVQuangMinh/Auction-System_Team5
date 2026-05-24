package auction_client.controllers;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import org.testfx.framework.junit5.ApplicationTest;

import auction_client.Network.ClientService;
import auction_client.UserSession;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.AuctionDTO;
import auction_shared.dto.AuctionStatus;
import auction_shared.dto.ItemDTO;
import auction_shared.dto.ItemType;
import auction_shared.dto.UserDTO;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;


public class SellProductSceneControllerTest extends ApplicationTest {

    private static final String TEST_USERNAME = "nhan";

    private SellProductSceneController controller;
    private ClientService mockClientService;
    private MockedStatic<ClientService> mockedStaticClientService;

    private final UserDTO testUser = new UserDTO("67", TEST_USERNAME, "USER");
    private final UserDTO otherUser = new UserDTO("68", "otherUser", "USER");

    private AuctionDTO createAuction(UserDTO owner, String itemId, String itemName) {
        ItemDTO item = new ItemDTO(itemId, itemName, "Mo ta san pham", owner, ItemType.VEHICLES);
        return new AuctionDTO(
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

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/SellProductScene.fxml")
        );
        Parent root = loader.load();
        controller = loader.getController();

        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    public void testInitialize() {
        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);

        verify(mockClientService).addListener(controller);
        verify(mockClientService).sendMessage(captor.capture());

        assertEquals("GET_MY_LIST", captor.getValue().getAction());
        assertEquals(TEST_USERNAME, captor.getValue().getData());
    }

    @Test
    public void testOverlayPane_HiddenByDefault() {
        AnchorPane overlay = lookup("#overlayPane").queryAs(AnchorPane.class);
        assertNotNull(overlay);
        assertFalse(overlay.isVisible());
    }

    @Test
    public void testOnUpdateReceived_GetMyList_PopulatesFlowPane() {
        AuctionDTO ownedAuction = createAuction(testUser, "01", "May tinh");
        NetworkMessage msg = new NetworkMessage("GET_MY_LIST", (Serializable) List.of(ownedAuction));

        interact(() -> controller.onUpdateReceived(msg));

        FlowPane flowPane = lookup("#myListFlowPane").queryAs(FlowPane.class);
        assertNotNull(flowPane);
        assertEquals(1, flowPane.getChildren().size());
    }

    @Test
    public void testOnUpdateReceived_UpdateBid_KeepsOnlyOwnedAuctions() {
        AuctionDTO ownedAuction = createAuction(testUser, "01", "San pham cua toi");
        AuctionDTO othersAuction = createAuction(otherUser, "02", "San pham nguoi khac");

        NetworkMessage msg = new NetworkMessage(
                "UPDATE_BID",
                (Serializable) List.of(ownedAuction, othersAuction)
        );

        interact(() -> controller.onUpdateReceived(msg));

        FlowPane flowPane = lookup("#myListFlowPane").queryAs(FlowPane.class);
        assertEquals(1, flowPane.getChildren().size());
    }

    @Test
    public void testOnUpdateReceived_GetMyList_EmptyList_ClearsFlowPane() {
        AuctionDTO ownedAuction = createAuction(testUser, "01", "May tinh");
        interact(() -> controller.onUpdateReceived(
                new NetworkMessage("GET_MY_LIST", (Serializable) List.of(ownedAuction))
        ));

        FlowPane flowPane = lookup("#myListFlowPane").queryAs(FlowPane.class);
        assertEquals(1, flowPane.getChildren().size());

        interact(() -> controller.onUpdateReceived(
                new NetworkMessage("GET_MY_LIST", (Serializable) new ArrayList<AuctionDTO>())
        ));

        assertEquals(0, flowPane.getChildren().size());
    }

    @Test
    public void testCleanup_RemovesListener() {
        interact(() -> controller.cleanup());
        verify(mockClientService).removeListener(controller);
    }
}

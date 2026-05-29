package auctionclient.controllers.admin;

import auctionclient.Network.ClientService;
import auctionclient.controllers.notification.UserPushUpNotificationController;
import auctionshared.Network.NetworkMessage;
import auctionshared.dto.AuctionStatus;
import auctionshared.dto.UserDTO;
import auctionshared.dto.ItemDTO;
import auctionshared.dto.AuctionDTO;
import auctionshared.dto.ItemType;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import auctionclient.controllers.FxControllerTestBase;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class AdminControlPanelControllerTest extends FxControllerTestBase {
    private AdminControlPanelController controller;
    private ClientService mockClientService;

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

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/auctionclient/AdminControlPanel.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    public void testInitialize() {
        Mockito.verify(mockClientService, times(1)).addListener(controller);
        ArgumentCaptor<NetworkMessage> messageCaptor = ArgumentCaptor.forClass(NetworkMessage.class);
        Mockito.verify(mockClientService, times(2)).sendMessage(messageCaptor.capture());
        List<NetworkMessage> msg = messageCaptor.getAllValues();
        assertEquals("GET_USERS", msg.get(0).getAction());
        assertEquals("GET_ACTIVE_PRODUCTS", msg.get(1).getAction());
    }

    @Test
    public void testOnUpdateReceived_UserTable() {
        UserDTO user1 = new UserDTO("1", "nam", "USER");
        UserDTO user2 = new UserDTO("2", "nhan", "ADMIN");
        NetworkMessage msg = new NetworkMessage("GET_USERS", (Serializable) Arrays.asList(user1, user2));

        controller.onUpdateReceived(msg);
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

        TableView<UserDTO> userTable = lookup("#userTable").queryAs(TableView.class);
        assertEquals(2, userTable.getItems().size());
        assertEquals("1", userTable.getItems().get(0).getId());
        assertEquals("2", userTable.getItems().get(1).getId());
        assertEquals("nam", userTable.getItems().get(0).getUsername());
        assertEquals("nhan", userTable.getItems().get(1).getUsername());
        assertEquals("USER", userTable.getItems().get(0).getRole());
        assertEquals("ADMIN", userTable.getItems().get(1).getRole());
    }

    @Test
    public void testOnUpdateReceived_ProductTable() {
        ItemDTO item1 = new ItemDTO("item-1", "Laptop", "desc", null, ItemType.ELECTRONICS);
        ItemDTO item2 = new ItemDTO("item-2", "Painting", "desc", null, ItemType.ARTS);
        AuctionDTO auction1 = new AuctionDTO(
                item1,
                AuctionStatus.ACTIVE,
                100.0,
                500.0,
                10.0,
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now().plusHours(1),
                false,
                null,
                150.0);
        AuctionDTO auction2 = new AuctionDTO(
                item2,
                AuctionStatus.ACTIVE,
                200.0,
                800.0,
                20.0,
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now().plusHours(2),
                false,
                null,
                250.0);
        NetworkMessage msg = new NetworkMessage(
                "GET_ACTIVE_PRODUCTS", (Serializable) Arrays.asList(auction1, auction2));

        controller.onUpdateReceived(msg);
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

        TableView<AuctionDTO> itemTable = lookup("#itemTable").queryAs(TableView.class);
        assertEquals(2, itemTable.getItems().size());
        assertEquals("Laptop", itemTable.getItems().get(0).getItem().getName());
        assertEquals("Painting", itemTable.getItems().get(1).getItem().getName());
        assertEquals(150.0, itemTable.getItems().get(0).getCurrentHighestBid());
        assertEquals(250.0, itemTable.getItems().get(1).getCurrentHighestBid());
        assertEquals(AuctionStatus.ACTIVE, itemTable.getItems().get(0).getStatus());
        assertEquals(AuctionStatus.ACTIVE, itemTable.getItems().get(1).getStatus());
    }

    @Test
    public void testOnUpdateReceived_AuctionEnded_Notification() {
        try (MockedStatic<UserPushUpNotificationController> mockedNotification = mockStatic(
                UserPushUpNotificationController.class)) {
            ItemDTO item = new ItemDTO(
                    "item-1", "Laptop", "desc", null, auctionshared.dto.ItemType.ELECTRONICS);
            AuctionDTO dto = new AuctionDTO(
                    item,
                    auctionshared.dto.AuctionStatus.ENDED,
                    100.0,
                    500.0,
                    10.0,
                    java.time.LocalDateTime.now(),
                    java.time.LocalDateTime.now(),
                    false,
                    null,
                    100.0);

            NetworkMessage msg = new NetworkMessage("AUCTION_ENDED", dto);

            controller.onUpdateReceived(msg);
            org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

            mockedNotification.verify(() -> UserPushUpNotificationController.showNotification(
                    "Phiên đấu giá kết thúc: Laptop", "INFO"), times(1));
        }
    }

}
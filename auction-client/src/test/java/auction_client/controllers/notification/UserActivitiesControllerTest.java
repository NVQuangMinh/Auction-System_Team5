package auction_client.controllers.notification;

import auction_client.Network.ClientService;
import auction_shared.Network.NetworkMessage;
import auction_shared.Network.Notification;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.testfx.framework.junit5.ApplicationTest;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserActivitiesControllerTest extends ApplicationTest {

    private UserActivitiesController controller;
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
                getClass().getResource("/auction_client/ActivitiesScene.fxml")
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

        assertEquals("GET_ACTIVITIES", captor.getValue().getAction());
        assertNull(captor.getValue().getData());
    }

    @Test
    public void testOnUpdateReceived_GetActivities_PopulatesNotificationContainer() {
        Notification first = new Notification("You placed a bid", LocalTime.of(9, 15));
        Notification second = new Notification("Auction ended", LocalTime.of(14, 30));
        NetworkMessage msg = new NetworkMessage(
                "GET_ACTIVITIES",
                (Serializable) List.of(first, second)
        );

        controller.onUpdateReceived(msg);
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

        VBox container = lookup("#notificationContainer").queryAs(VBox.class);
        assertNotNull(container);
        assertEquals(2, container.getChildren().size());

        HBox topItem = (HBox) container.getChildren().get(0);
        Label topMessage = (Label) topItem.getChildren().get(0);
        Label topTime = (Label) topItem.getChildren().get(2);
        assertEquals(second.getNotificationMessage(), topMessage.getText());
        assertEquals(String.valueOf(second.getNotificationTime()), topTime.getText());

        HBox bottomItem = (HBox) container.getChildren().get(1);
        Label bottomMessage = (Label) bottomItem.getChildren().get(0);
        Label bottomTime = (Label) bottomItem.getChildren().get(2);
        assertEquals(first.getNotificationMessage(), bottomMessage.getText());
        assertEquals(String.valueOf(first.getNotificationTime()), bottomTime.getText());
    }

    @Test
    public void testOnUpdateReceived_GetActivities_EmptyList_ClearsContainer() {
        Notification notification = new Notification("Existing activity", LocalTime.of(8, 0));
        controller.onUpdateReceived(
                new NetworkMessage("GET_ACTIVITIES", (Serializable) List.of(notification))
        );
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

        VBox container = lookup("#notificationContainer").queryAs(VBox.class);
        assertEquals(1, container.getChildren().size());

        controller.onUpdateReceived(
                new NetworkMessage("GET_ACTIVITIES", (Serializable) new ArrayList<Notification>())
        );
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

        assertEquals(0, container.getChildren().size());
    }

    @Test
    public void testOnUpdateReceived_GetActivities_CaseInsensitive() {
        Notification notification = new Notification("New notification", LocalTime.of(12, 0));
        NetworkMessage msg = new NetworkMessage(
                "get_activities",
                (Serializable) List.of(notification)
        );

        controller.onUpdateReceived(msg);
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

        VBox container = lookup("#notificationContainer").queryAs(VBox.class);
        assertEquals(1, container.getChildren().size());

        HBox item = (HBox) container.getChildren().get(0);
        Label message = (Label) item.getChildren().get(0);
        assertEquals("New notification", message.getText());
    }

    @Test
    public void testOnUpdateReceived_OtherAction_DoesNotChangeContainer() {
        controller.onUpdateReceived(
                new NetworkMessage("GET_ACTIVITIES", (Serializable) List.of(
                        new Notification("Activity", LocalTime.of(10, 0))
                ))
        );
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

        VBox container = lookup("#notificationContainer").queryAs(VBox.class);
        assertEquals(1, container.getChildren().size());

        controller.onUpdateReceived(
                new NetworkMessage("BID_SUCCESS", null)
        );
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

        assertEquals(1, container.getChildren().size());
    }

    @Test
    public void testLoadNotifications_RefreshesItems() {
        Notification notification = new Notification("Reloaded", LocalTime.of(16, 45));
        controller.onUpdateReceived(
                new NetworkMessage("GET_ACTIVITIES", (Serializable) List.of(notification))
        );
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

        VBox container = lookup("#notificationContainer").queryAs(VBox.class);
        assertEquals(1, container.getChildren().size());

        interact(() -> controller.loadNotifications());

        assertEquals(1, container.getChildren().size());
        HBox item = (HBox) container.getChildren().get(0);
        Label message = (Label) item.getChildren().get(0);
        assertEquals("Reloaded", message.getText());
    }
}

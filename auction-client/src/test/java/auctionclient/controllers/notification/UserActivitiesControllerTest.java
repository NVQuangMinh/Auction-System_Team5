package auctionclient.controllers.notification;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import org.testfx.framework.junit5.ApplicationTest;

import auctionclient.Network.ClientService;
import auctionshared.Network.NetworkMessage;
import auctionshared.Network.Notification;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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
                getClass().getResource("/auctionclient/ActivitiesScene.fxml")
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
    public void testOnUpdateReceived_GetActivities_AllNotificationsRenderedInOrder() {
        Notification first = new Notification("You placed a bid", LocalTime.of(9, 15));
        Notification second = new Notification("Auction ended", LocalTime.of(14, 30));
        Notification third = new Notification("You won the auction", LocalTime.of(18, 0));

        NetworkMessage msg = new NetworkMessage(
                "GET_ACTIVITIES",
                (Serializable) List.of(first, second, third)
        );

        controller.onUpdateReceived(msg);
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

        VBox container = lookup("#notificationContainer").queryAs(VBox.class);
        assertNotNull(container);
        assertEquals(3, container.getChildren().size());

        // Items should be added in reverse order (latest first)
        HBox firstItem = (HBox) container.getChildren().get(0);
        Label firstMessage = (Label) firstItem.getChildren().get(0);
        Label firstTime = (Label) firstItem.getChildren().get(2);
        assertEquals(third.getNotificationMessage(), firstMessage.getText());
        assertEquals(String.valueOf(third.getNotificationTime()), firstTime.getText());

        HBox secondItem = (HBox) container.getChildren().get(1);
        Label secondMessage = (Label) secondItem.getChildren().get(0);
        Label secondTime = (Label) secondItem.getChildren().get(2);
        assertEquals(second.getNotificationMessage(), secondMessage.getText());
        assertEquals(String.valueOf(second.getNotificationTime()), secondTime.getText());

        HBox thirdItem = (HBox) container.getChildren().get(2);
        Label thirdMessage = (Label) thirdItem.getChildren().get(0);
        Label thirdTime = (Label) thirdItem.getChildren().get(2);
        assertEquals(first.getNotificationMessage(), thirdMessage.getText());
        assertEquals(String.valueOf(first.getNotificationTime()), thirdTime.getText());
    }
}

package auctionclient.controllers.notification;

import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.javafx.FontIcon;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.matcher.control.LabeledMatchers;

import auctionclient.Network.ClientService;
import auctionclient.UserSession;
import auctionshared.Network.NetworkMessage;
import auctionshared.dto.UserDTO;
import javafx.animation.Animation;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

class UserPushUpNotificationControllerTest extends ApplicationTest {

    private static final String TEST_USERNAME = "nhan";

    private UserPushUpNotificationController controller;
    private ClientService mockClientService;

    @AfterEach
    public void closeMock() throws Exception {
        resetUserSession();
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

        UserSession.getInstance().setUsername(TEST_USERNAME);

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/auctionclient/UserPushUpNotification.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        setNotificationStage(controller, stage);

        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    public void testSetNotification_Success() {
        interact(() -> controller.setNotification("Bid placed!", "SUCCESS"));

        FxAssert.verifyThat("#notificationLabel", LabeledMatchers.hasText("Bid placed!"));

        FontIcon icon = lookup("#notificationIcon").queryAs(FontIcon.class);
        assertEquals("fas-check-circle", icon.getIconLiteral());
        assertEquals(Color.web("#2ecc71"), icon.getIconColor());

        Rectangle progressFill = lookup("#progressFill").queryAs(Rectangle.class);
        assertEquals(Color.web("#2ecc71"), progressFill.getFill());

        Rectangle progressBackground = lookup("#progressBackground").queryAs(Rectangle.class);
        assertEquals(progressBackground.getWidth(), progressFill.getWidth(), 0.01);
    }

    @Test
    public void testSetNotification_Failed() {
        interact(() -> controller.setNotification("Bid failed", "FAILED"));

        FxAssert.verifyThat("#notificationLabel", LabeledMatchers.hasText("Bid failed"));

        FontIcon icon = lookup("#notificationIcon").queryAs(FontIcon.class);
        assertEquals("fas-times-circle", icon.getIconLiteral());
        assertEquals(Color.web("#e74c3c"), icon.getIconColor());

        Rectangle progressFill = lookup("#progressFill").queryAs(Rectangle.class);
        assertEquals(Color.web("#e74c3c"), progressFill.getFill());
    }

    @Test
    public void testSetNotification_DefaultType() {
        interact(() -> controller.setNotification("New update", "INFO"));

        FxAssert.verifyThat("#notificationLabel", LabeledMatchers.hasText("New update"));

        FontIcon icon = lookup("#notificationIcon").queryAs(FontIcon.class);
        assertEquals("fas-bell", icon.getIconLiteral());
        assertEquals(Color.web("#f1c40f"), icon.getIconColor());

        Rectangle progressFill = lookup("#progressFill").queryAs(Rectangle.class);
        assertEquals(Color.web("#f1c40f"), progressFill.getFill());
    }

    @Test
    public void testPlayAnimation_StartsTimeline() throws Exception {
        interact(() -> {
            controller.setNotification("Running", "SUCCESS");
            controller.playAnimation();
        });

        Timeline timeline = getTimeline(controller);
        assertEquals(Animation.Status.RUNNING, timeline.getStatus());
    }

    @Test
    public void testOnUpdateReceived_BidSuccess() {
        try (MockedStatic<UserPushUpNotificationController> mocked =
                     mockStatic(UserPushUpNotificationController.class)) {
            controller.onUpdateReceived(new NetworkMessage("BID_SUCCESS", null));
            org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

            mocked.verify(() -> UserPushUpNotificationController.showNotification(
                    "You have placed bid successfully", "SUCCESS"));
        }
    }

    @Test
    public void testOnUpdateReceived_BidFailed() {
        try (MockedStatic<UserPushUpNotificationController> mocked =
                     mockStatic(UserPushUpNotificationController.class)) {
            controller.onUpdateReceived(
                    new NetworkMessage("BID_FAILED", "Insufficient balance"));
            org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

            mocked.verify(() -> UserPushUpNotificationController.showNotification(
                    "Insufficient balance", "FAILED"));
        }
    }

    @Test
    public void testOnUpdateReceived_SellSuccess() {
        try (MockedStatic<UserPushUpNotificationController> mocked =
                     mockStatic(UserPushUpNotificationController.class)) {
            controller.onUpdateReceived(new NetworkMessage("SELL_SUCCESS", null));
            org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

            mocked.verify(() -> UserPushUpNotificationController.showNotification(
                    "You have sold item successfully", "SUCCESS"));
        }
    }

    @Test
    public void testOnUpdateReceived_SellFailed() {
        try (MockedStatic<UserPushUpNotificationController> mocked =
                     mockStatic(UserPushUpNotificationController.class)) {
            controller.onUpdateReceived(
                    new NetworkMessage("SELL_FAILED", "Cannot list item"));
            org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

            mocked.verify(() -> UserPushUpNotificationController.showNotification(
                    "Cannot list item", "FAILED"));
        }
    }

    @Test
    public void testOnUpdateReceived_BuyoutSuccess() {
        try (MockedStatic<UserPushUpNotificationController> mocked =
                     mockStatic(UserPushUpNotificationController.class)) {
            controller.onUpdateReceived(new NetworkMessage("BUYOUT_SUCCESS", null));
            org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

            mocked.verify(() -> UserPushUpNotificationController.showNotification(
                    "You have buy out item successfully", "SUCCESS"));
        }
    }

    @Test
    public void testOnUpdateReceived_BuyoutFailed() {
        try (MockedStatic<UserPushUpNotificationController> mocked =
                     mockStatic(UserPushUpNotificationController.class)) {
            controller.onUpdateReceived(
                    new NetworkMessage("BUYOUT_FAILED", "Buyout not allowed"));
            org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

            mocked.verify(() -> UserPushUpNotificationController.showNotification(
                    "Buyout not allowed", "FAILED"));
        }
    }

    @Test
    public void testOnUpdateReceived_BanUser_OtherUsername() {
        clearInvocations(mockClientService);
        controller.onUpdateReceived(new NetworkMessage("BAN_USER",
                new UserDTO("2", "otherUser", "USER")));
        verify(mockClientService, never()).sendMessage(any());
    }

    private void resetUserSession() throws Exception {
        Field selfField = UserSession.class.getDeclaredField("self");
        selfField.setAccessible(true);
        selfField.set(null, null);
        UserSession.getInstance().setUsername(TEST_USERNAME);
    }

    private void setNotificationStage(UserPushUpNotificationController target, Stage stage)
            throws Exception {
        Field field = UserPushUpNotificationController.class.getDeclaredField("notificationStage");
        field.setAccessible(true);
        field.set(target, stage);
    }

    private Timeline getTimeline(UserPushUpNotificationController target) throws Exception {
        Field field = UserPushUpNotificationController.class.getDeclaredField("timeline");
        field.setAccessible(true);
        return (Timeline) field.get(target);
    }
}

package auctionclient.controllers.notification;

import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.javafx.FontIcon;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;
import org.testfx.api.FxAssert;
import auctionclient.controllers.FxControllerTestBase;
import org.testfx.matcher.control.LabeledMatchers;

import auctionclient.UserSession;
import auctionshared.Network.NetworkMessage;
import javafx.animation.Animation;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

class UserPushUpNotificationControllerTest extends FxControllerTestBase {

    private static final String TEST_USERNAME = "nhan";

    private UserPushUpNotificationController controller;

    @AfterEach
    public void closeMock() throws Exception {
        resetUserSession();
    }

    @Override
    public void start(Stage stage) throws Exception {
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

        Rectangle progressBackground = lookup("#progressBackground").queryAs(Rectangle.class);
        assertEquals(progressBackground.getWidth(), progressFill.getWidth(), 0.01);
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

        Rectangle progressBackground = lookup("#progressBackground").queryAs(Rectangle.class);
        assertEquals(progressBackground.getWidth(), progressFill.getWidth(), 0.01);
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
                    "Bạn đã trả giá sản phẩm thành công.", "SUCCESS"));
        }
    }

    @Test
    public void testOnUpdateReceived_BidFailed() {
        try (MockedStatic<UserPushUpNotificationController> mocked =
                     mockStatic(UserPushUpNotificationController.class)) {
            controller.onUpdateReceived(
                    new NetworkMessage("BID_FAILED", "Trả giá thất bại: không tìm thấy phiên đấu giá."));
            org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

            mocked.verify(() -> UserPushUpNotificationController.showNotification(
                    "Trả giá thất bại: không tìm thấy phiên đấu giá.", "FAILED"));
        }
    }

    @Test
    public void testOnUpdateReceived_SellSuccess() {
        try (MockedStatic<UserPushUpNotificationController> mocked =
                     mockStatic(UserPushUpNotificationController.class)) {
            controller.onUpdateReceived(new NetworkMessage("SELL_SUCCESS", null));
            org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

            mocked.verify(() -> UserPushUpNotificationController.showNotification(
                    "Bạn đã đăng bán sản phẩm thành công.", "SUCCESS"));
        }
    }

    @Test
    public void testOnUpdateReceived_SellFailed() {
        try (MockedStatic<UserPushUpNotificationController> mocked =
                     mockStatic(UserPushUpNotificationController.class)) {
            controller.onUpdateReceived(
                    new NetworkMessage("SELL_FAILED", "Bạn đã đăng bán sản phẩm thất bại."));
            org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

            mocked.verify(() -> UserPushUpNotificationController.showNotification(
                    "Bạn đã đăng bán sản phẩm thất bại.", "FAILED"));
        }
    }

    @Test
    public void testOnUpdateReceived_BuyoutSuccess() {
        try (MockedStatic<UserPushUpNotificationController> mocked =
                     mockStatic(UserPushUpNotificationController.class)) {
            controller.onUpdateReceived(new NetworkMessage("BUYOUT_SUCCESS", null));
            org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

            mocked.verify(() -> UserPushUpNotificationController.showNotification(
                    "Bạn đã mua sản phẩm thành công.", "SUCCESS"));
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

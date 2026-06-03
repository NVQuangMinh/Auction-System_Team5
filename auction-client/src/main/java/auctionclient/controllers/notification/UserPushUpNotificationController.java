package auctionclient.controllers.notification;

import org.kordamp.ikonli.javafx.FontIcon;

import auctionclient.UserSession;
import auctionclient.interfaces.AuctionUpdateListener;
import auctionshared.Network.NetworkMessage;
import auctionshared.dto.UserDTO;
import auctionshared.dto.AuctionDTO;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

public class UserPushUpNotificationController implements AuctionUpdateListener {
    private static final java.util.List<Stage> activeNotifications = new java.util.ArrayList<>();

    private static void recalculatePositions() {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        for (int i = 0; i < activeNotifications.size(); i++) {
            Stage stage = activeNotifications.get(i);
            double yPos = screenBounds.getMaxY() - 54 - 20 - (i * (54 + 10));
            stage.setY(yPos);
        }
    }

    @FXML
    private StackPane iconContainer;
    @FXML
    private Label notificationLabel;
    @FXML
    private FontIcon notificationIcon;
    @FXML
    private Rectangle progressBackground;
    @FXML
    private Rectangle progressFill;

    private double totalTimeMs = 3000; // thoi gian an
    private Stage notificationStage;
    private Timeline timeline;
    private double progress = 1.0;

    public static void showNotification(String notification, String type) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(UserPushUpNotificationController.class
                        .getResource("/auctionclient/UserPushUpNotification.fxml"));

                Scene notificationWindow = new Scene(loader.load());
                notificationWindow.setFill(null); // nen trong suot

                Stage newNotificationStage = new Stage();
                newNotificationStage.initStyle(StageStyle.TRANSPARENT); // xoa thanh cua so
                newNotificationStage.setScene(notificationWindow);

                newNotificationStage.setAlwaysOnTop(true);

                UserPushUpNotificationController controller = loader.getController();
                controller.notificationStage = newNotificationStage;

                controller.setNotification(notification, type);
                Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

                newNotificationStage.setX(screenBounds.getMaxX() - 500 - 20);
                activeNotifications.add(newNotificationStage);
                recalculatePositions();

                newNotificationStage.show();
                controller.playAnimation(); // Chạy animation sau khi Stage đã hiển thị
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void setNotification(String notification, String type) {
        this.notificationLabel.setText(notification);

        if ("SUCCESS".equalsIgnoreCase(type)) {
            this.notificationIcon.setIconLiteral("fas-check-circle");
            this.notificationIcon.setIconColor(Color.web("#2ecc71"));
            progressFill.setFill(Color.web("#2ecc71"));
        } else if ("FAILED".equalsIgnoreCase(type)) {
            this.notificationIcon.setIconLiteral("fas-times-circle");
            this.notificationIcon.setIconColor(Color.web("#e74c3c"));
            progressFill.setFill(Color.web("#e74c3c"));
        } else {
            this.notificationIcon.setIconLiteral("fas-bell");
            this.notificationIcon.setIconColor(Color.web("#f1c40f"));
            progressFill.setFill(Color.web("#f1c40f"));
        }

        // 3. Khởi tạo Timeline để chạy đếm ngược
        progress = 1.0; // Reset lại tiến trình mỗi lần mở alert
        progressFill.setWidth(progressBackground.getWidth());

        double updateInterval = 0.01;
        double decrement = updateInterval / (totalTimeMs / 1000.0);

        this.timeline = new Timeline();
        KeyFrame keyFrame = new KeyFrame(Duration.seconds(updateInterval), event -> {
            progress -= decrement;

            if (progress <= 0) {
                progress = 0;
                timeline.stop();
                if (notificationStage != null) {
                    notificationStage.close();
                    activeNotifications.remove(notificationStage);
                    recalculatePositions();
                }
            }
            progressFill.setWidth(progress * progressBackground.getWidth());
        });

        timeline.getKeyFrames().add(keyFrame);
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    public void playAnimation() {
        if (timeline != null && timeline.getStatus() != Animation.Status.RUNNING) {
            timeline.play();
        }
    }

    @Override
    public void onUpdateReceived(NetworkMessage msg) {
        String action = msg.getAction();
        if ("LOGIN".equals(action) && msg.getData() != null) {
            UserPushUpNotificationController.showNotification("Đăng nhập thành công.", "SUCCESS");
        }
        if ("CREATE_ACCOUNT".equals(action) && (boolean) msg.getData()) {
            UserPushUpNotificationController.showNotification("Đã tạo tài khoản mới thành công.", "SUCCESS");
        }
        if ("BID_SUCCESS".equals(action)) {
            UserPushUpNotificationController.showNotification("Bạn đã trả giá sản phẩm thành công.", "SUCCESS");
        } else if ("BID_FAILED".equals(action)) {
            if (msg.getData() == null) {
                UserPushUpNotificationController.showNotification("Trả giá thất bại: không tìm thấy người dùng.", "FAILED");
            } else {
                UserPushUpNotificationController.showNotification((String) msg.getData(), "FAILED");
            }
        }
        if ("SELL_SUCCESS".equals(action)) {
            UserPushUpNotificationController.showNotification("Bạn đã đăng bán sản phẩm thành công.", "SUCCESS");
        } else if ("SELL_FAILED".equals(action)) {
            UserPushUpNotificationController.showNotification("Bạn đã đăng bán sản phẩm thất bại.", "FAILED");
        }
        if ("BUYOUT_SUCCESS".equals(action)) {
            UserPushUpNotificationController.showNotification("Bạn đã mua sản phẩm thành công.", "SUCCESS");
        } else if ("BUYOUT_FAILED".equals(action)) {
            UserPushUpNotificationController.showNotification((String) msg.getData(), "FAILED");
        }
        if ("BAN_USER".equals(action)) {
            UserDTO userDTO = (UserDTO) msg.getData();
            if (userDTO.getUsername().equals(UserSession.getInstance().getUsername())) {
                try {
                    UserPushUpNotificationController.showNotification("Tài khoản của bạn đã bị khoá.", "NOTIFY");
                    // Đóng tất cả stage
                    Timeline timeline = new Timeline(new KeyFrame(
                            Duration.seconds(3),
                            event -> {
                                Platform.exit();
                            }
                    ));
                    timeline.play();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            } else if (UserSession.getInstance().getUser().getRole().equals("ADMIN")) {
                UserPushUpNotificationController.showNotification("Khoá tài khoản người dùng thành công.", "SUCCESS");
            }
        }
        if ("REMOVE_ITEM".equals(action)) {
            UserPushUpNotificationController.showNotification("Gỡ bỏ sản phẩm thành công.", "SUCCESS");
        }
        if ("AUCTION_ENDED".equals(action) || "AUCTION_SOLD".equals(action)) {
            if (msg.getData() instanceof AuctionDTO dto) {
                UserPushUpNotificationController.showNotification(
                        "Phiên đấu giá kết thúc: " + dto.getItem().getName(), "INFO");
            }
        }
        if ("YOU_WON".equals(action)) {
            if (msg.getData() instanceof AuctionDTO dto) {
                UserPushUpNotificationController.showNotification(
                        "Bạn đã thắng phiên đấu giá: " + dto.getItem().getName(), "SUCCESS");
            }
        }
    }
}

package auction_client.controllers;

import org.kordamp.ikonli.javafx.FontIcon;

import auction_client.UserSession;
import auction_client.interfaces.AuctionUpdateListener;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.UserDTO;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
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
    private double timeLeftMs = totalTimeMs;
    private Stage notificationStage;
    private Timeline timeline;
    private double progress = 1.0;

    public static void showNotification(String notification, String type) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(UserPushUpNotificationController.class
                        .getResource("/auction_client/UserPushUpNotification.fxml"));

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

                // Căn chỉnh góc phải dưới chính xác với margin 20px
                // chiều cao thực tế của FXML là 54px
                newNotificationStage.setX(screenBounds.getMaxX() - 437 - 20);
                newNotificationStage.setY(screenBounds.getMaxY() - 54 - 20);

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
        if ("BID_SUCCESS".equals(action)) {
            UserPushUpNotificationController.showNotification("You have placed bid successfully", "SUCCESS");
        } else if ("BID_FAILED".equals(action)) {
            UserPushUpNotificationController.showNotification((String) msg.getData(), "FAILED");
        }
        if ("SELL_SUCCESS".equals(action)) {
            UserPushUpNotificationController.showNotification("You have sold item successfully", "SUCCESS");
        } else if ("SELL_FAILED".equals(action)) {
            UserPushUpNotificationController.showNotification((String) msg.getData(), "FAILED");
        }
        if ("BUYOUT_SUCCESS".equals(action)) {
            UserPushUpNotificationController.showNotification("You have buy out item successfully", "SUCCESS");
        } else if ("BUYOUT_FAILED".equals(action)) {
            UserPushUpNotificationController.showNotification((String) msg.getData(), "FAILED");
        }
        if ("BAN_USER".equals(action)) {
            UserDTO userDTO = (UserDTO) msg.getData();
            if (userDTO.getUsername().equals(UserSession.getInstance().getUsername())) {
                try {
                    // Đóng tất cả stage
                    if (!Stage.getWindows().isEmpty()) {
                        for (Window window : Stage.getWindows()) {
                            if (window instanceof Stage) {
                                ((Stage) window).close();
                            }
                        }

                        UserSession.getInstance().closeApp();
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
    }
}

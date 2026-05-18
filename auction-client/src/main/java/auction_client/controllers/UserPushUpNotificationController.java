package auction_client.controllers;

import auction_client.interfaces.AuctionUpdateListener;
import auction_shared.Network.NetworkMessage;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

public class UserPushUpNotificationController implements AuctionUpdateListener {
    @FXML
    private StackPane iconContainer;
    @FXML
    private Label notificationLabel;
    @FXML
    private ProgressBar progressBar;

    private double totalTimeMs = 3000; // thoi gian an
    private double timeLeftMs = totalTimeMs;
    private Stage notificationStage;
    private Timeline timeline;

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

                // Căn chỉnh góc phải dưới chính xác với margin 20px (chiều rộng FXML là 437px,
                // chiều cao là 100px)
                newNotificationStage.setX(screenBounds.getMaxX() - 437 - 20);
                newNotificationStage.setY(screenBounds.getMaxY() - 100 - 20);

                newNotificationStage.show();
                controller.playAnimation(); // Chạy animation sau khi Stage đã hiển thị
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void setNotification(String notification, String type) {
        this.notificationLabel.setText(notification);
        this.progressBar.setMaxWidth(Double.MAX_VALUE); // Cho phép ProgressBar giãn hết chiều ngang của VBox

        FontIcon icon = new FontIcon();
        icon.setIconSize(18);

        if ("SUCCESS".equalsIgnoreCase(type)) {
            icon.setIconLiteral("fas-check-circle");
            icon.setStyle("-fx-icon-color: #2ecc71;");
            progressBar.setStyle("-fx-accent: #2ecc71;");
        } else if ("FAILED".equalsIgnoreCase(type)) {
            icon.setIconLiteral("fas-check-xmark");
            icon.setStyle("-fx-icon-color: #e74c3c;");
            progressBar.setStyle("-fx-accent: #e74c3c;");
        } else {
            // Loại thông báo chung / Cảnh báo
            icon.setIconLiteral("fas-bell");
            icon.setStyle("-fx-icon-color: #f1c40f;");
            progressBar.setStyle("-fx-accent: #f1c40f;");
        }

        this.iconContainer.getChildren().clear();
        this.iconContainer.getChildren().add(icon);

        progressBar.setProgress(1.0);

        // 2. Tạo hiệu ứng: Sau đúng 3 giây, tự động kéo giá trị progressBar về RỖNG
        // (0.0)
        // Xác định rõ ràng điểm bắt đầu là 1.0 và kết thúc là 0.0
        this.timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progressBar.progressProperty(), 1.0)),
                new KeyFrame(Duration.seconds(3), new KeyValue(progressBar.progressProperty(), 0.0)));

        // 3. Khi hiệu ứng chạy xong 3 giây thì tự đóng cửa sổ
        timeline.setOnFinished(e -> {
            if (notificationStage != null) {
                notificationStage.close();
            }
        });
    }

    public void playAnimation() {
        if (timeline != null) {
            timeline.play();
        }
    }

    @Override
    public void onUpdateReceived(NetworkMessage msg) {
        String action = msg.getAction();
        if ("BID_SUCCESS".equals(action)) {
            UserPushUpNotificationController.showNotification("You have placed bid successfully", "SUCCESS");
        } else if ("BID_FAILED".equals(action)) {
            UserPushUpNotificationController.showNotification("Your bid has failed", "FAILED");
        }
        if ("SELL_SUCCESS".equals(action)) {
            UserPushUpNotificationController.showNotification("You have sold item successfully", "SUCCESS");
        } else if ("SELL_FAILED".equals(action)) {
            UserPushUpNotificationController.showNotification("Sell item failed", "FAILED");
        }
        if ("BUYOUT_SUCCESS".equals(action)) {
            UserPushUpNotificationController.showNotification("You have buy out item successfully", "SUCCESS");
        } else if ("BUYOUT_FAILED".equals(action)) {
            UserPushUpNotificationController.showNotification("Buyout failed", "FAILED");
        }
    }
}

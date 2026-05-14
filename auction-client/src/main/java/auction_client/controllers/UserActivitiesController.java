package auction_client.controllers;

import auction_shared.Network.Notification;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class UserActivitiesController {
    @FXML
    private VBox notificationContainer;

    public void loadNotifications(List<Notification> notificationList) {
        notificationContainer.getChildren().clear();
        for (Notification notification : notificationList) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/ActivitiesItem.fxml"));
                Parent item = loader.load();

                ActivitiesItemController controller = loader.getController();
                controller.setData(notification.getNotificationMSG(), notification.getNotificationTime());
                notificationContainer.getChildren().add(item);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

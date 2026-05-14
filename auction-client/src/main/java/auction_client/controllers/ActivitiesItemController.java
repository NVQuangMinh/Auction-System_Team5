package auction_client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ActivitiesItemController {
    @FXML
    private Label notification;
    @FXML
    private Label notificationTime;

    @FXML
    public void setData(String notification, String notificationTime) {
        this.notification.setText(notification);
        this.notificationTime.setText(notificationTime);
    }
}

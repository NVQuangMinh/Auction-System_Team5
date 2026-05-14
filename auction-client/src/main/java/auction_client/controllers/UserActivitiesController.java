package auction_client.controllers;

import auction_shared.Network.Notification;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class UserActivitiesController {
    @FXML
    private VBox notificationContainer;

    public void loadNotifications(List<Notification> notificationList) {
        notificationContainer.getChildren().clear();
        for
    }
}

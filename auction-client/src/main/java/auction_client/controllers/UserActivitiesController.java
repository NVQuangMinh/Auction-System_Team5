package auction_client.controllers;

import auction_client.Network.ClientService;
import auction_client.interfaces.AuctionUpdateListener;
import auction_shared.Network.NetworkMessage;
import auction_shared.Network.Notification;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class UserActivitiesController implements AuctionUpdateListener, Initializable {
    @FXML
    VBox notificationContainer;

    private List<Notification> activities;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ClientService.getInstance().addListener(this);
        ClientService.getInstance().sendMessage(new NetworkMessage("GET_ACTIVITIES", null));
    }

    public void loadNotifications() {
        notificationContainer.getChildren().clear();
        for (Notification notification : activities) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/ActivitiesItem.fxml"));
                Parent item = loader.load();

                ActivitiesItemController controller = loader.getController();
                controller.setData(notification.getNotificationMSG(), String.valueOf(notification.getNotificationTime()));
                notificationContainer.getChildren().addFirst(item);
            } catch (IOException e) {
                System.out.println("Error loading notification item: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onUpdateReceived(NetworkMessage msg) {
        String action = msg.getAction();
        if ("GET_ACTIVITIES".equalsIgnoreCase(action)) {
            this.activities = (List<Notification>) msg.getData();
            System.out.println(this.activities.size());
            Platform.runLater(() -> loadNotifications());
        }
    }
}

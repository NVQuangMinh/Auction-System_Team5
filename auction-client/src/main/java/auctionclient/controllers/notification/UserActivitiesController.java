package auctionclient.controllers.notification;

import auctionclient.Network.ClientService;
import auctionclient.interfaces.AuctionUpdateListener;
import auctionclient.interfaces.Cleanable;
import auctionshared.Network.NetworkMessage;
import auctionshared.Network.Notification;

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

public class UserActivitiesController implements AuctionUpdateListener, Initializable, Cleanable {
    @FXML
    VBox notificationContainer;

    private List<Notification> activities;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ClientService.getInstance().addListener(this);
        ClientService.getInstance().sendMessage(new NetworkMessage("GET_ACTIVITIES", null));
    }

    @Override
    public void cleanup() {
        ClientService.getInstance().removeListener(this);
    }

    public void loadNotifications() {
        notificationContainer.getChildren().clear();
        for (Notification notification : activities) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/auctionclient/ActivitiesItem.fxml"));
                Parent item = loader.load();

                ActivitiesItemController controller = loader.getController();
                controller.setData(notification.getNotificationMessage(), String.valueOf(notification.getNotificationTime()));
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

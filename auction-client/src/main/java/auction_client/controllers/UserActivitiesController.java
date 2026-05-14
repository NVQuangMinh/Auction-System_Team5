package auction_client.controllers;

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

public class UserActivitiesController  implements AuctionUpdateListener {
    @FXML
    private VBox notificationContainer;

    public void loadNotifications(Notification notification) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/ActivitiesItem.fxml"));
            Parent item = loader.load();

            ActivitiesItemController controller = loader.getController();
            controller.setData(notification.getNotificationMSG(), String.valueOf(notification.getNotificationTime()));
            notificationContainer.getChildren().add(item);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onUpdateReceived(NetworkMessage msg) {
        String action = msg.getAction();
        if ("ACTIVITY".equals(action)) {
            Notification notification = (Notification) msg.getData();
            Platform.runLater(()-> {
                loadNotifications(notification);
            });
        }
    }
}

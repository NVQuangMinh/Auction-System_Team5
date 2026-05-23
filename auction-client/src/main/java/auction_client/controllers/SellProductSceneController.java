package auction_client.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import auction_client.Network.ClientService;
import auction_client.UserSession;
import auction_client.interfaces.AuctionUpdateListener;
import auction_client.interfaces.HandleCardClicked;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.AuctionDTO;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SellProductSceneController implements Initializable, AuctionUpdateListener, HandleCardClicked {
    @FXML
    public AnchorPane overlayPane;
    @FXML
    FlowPane myListFlowPane;

    // Always use a mutable list so we can replace its contents cleanly
    private List<AuctionDTO> myAuctions = new ArrayList<>();

    public void initialize(URL url, ResourceBundle resourceBundle) {
        ClientService.getInstance().addListener(this);
        ClientService.getInstance().sendMessage(new NetworkMessage("GET_MY_LIST", UserSession.getInstance().getUsername()));
    }

    public void cleanup() {
        ClientService.getInstance().removeListener(this);
    }

    @FXML
    public void handleUserAddItem(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/auction_client/ProductInfoSubmission.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        Stage owner = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.initOwner(owner);
        stage.setOnHiding(e -> overlayPane.setVisible(false));
        stage.setScene(scene);
        stage.setTitle("Add New Product");
        overlayPane.setVisible(true);
        stage.show();
    }

    private void updateMyList(List<AuctionDTO> auctions) {
        // Already on JavaFX thread via Platform.runLater
        myListFlowPane.getChildren().clear();
        for (AuctionDTO auction : auctions) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/ProductCard.fxml"));
                Parent card = loader.load();
                ProductCardController cardController = loader.getController();
                cardController.setData(auction, this::openAuctionDetail);
                myListFlowPane.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void onUpdateReceived(NetworkMessage msg) {
        String action = msg.getAction();
        if (action.equals("GET_MY_LIST")) {
            this.myAuctions = new ArrayList<>((List<AuctionDTO>) msg.getData());
            Platform.runLater(() -> updateMyList(myAuctions));
        } else if (action.equals("UPDATE_BID")) {
            List<AuctionDTO> rooms = (List<AuctionDTO>) msg.getData();
            // Tạo lại list
            List<AuctionDTO> rebuilt = new ArrayList<>();
            for (AuctionDTO room : rooms) {
                if (room.getItem().getOwner().getUsername().equals(UserSession.getInstance().getUsername())) {
                    rebuilt.add(room);
                }
            }
            this.myAuctions = rebuilt;
            Platform.runLater(() -> updateMyList(myAuctions));
        }
    }

    public void openAuctionDetail(AuctionDTO auction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/SellProductInfo.fxml"));
            Parent root = loader.load();

            SellProductInfoController controller = loader.getController();
            controller.initData(auction);

            Stage sellProductInfoStage = new Stage();
            sellProductInfoStage.setTitle("Auction Detail");
            sellProductInfoStage.initModality(Modality.APPLICATION_MODAL);
            sellProductInfoStage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(null);

            sellProductInfoStage.setScene(scene);
            sellProductInfoStage.centerOnScreen();
            sellProductInfoStage.setOnCloseRequest(event -> controller.cleanUp());
            sellProductInfoStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

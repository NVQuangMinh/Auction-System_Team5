package auction_client.controllers;

import auction_client.AuctionUpdateListener;
import auction_client.Network.ClientService;
import auction_shared.Network.NetworkMessage;
import auction_shared.entities.Auction;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class BidProductSceneController implements Initializable, AuctionUpdateListener {
    @FXML
    private FlowPane productFlowPane;


    public void initialize(URL url, ResourceBundle resourceBundle) {
        ClientService.getInstance().addListener(this);
        ClientService.getInstance().sendMessage(new NetworkMessage("GET_PRODUCTS", null));
    }

    public void updateProductList(List<Auction> auctions) {
        Platform.runLater(() -> {
            // 1. Xóa các card cũ để tránh trùng lặp khi cập nhật
            productFlowPane.getChildren().clear();

            for (Auction auction : auctions) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/ProductCard.fxml"));
                    Parent card = loader.load();

                    ProductCardController cardController = loader.getController();
                    cardController.setData(auction);

                    card.setOnMouseClicked(event -> {
                        if (event.getClickCount() == 2){ //double click nha
                            openAuctionDetail(auction);
                        }
                    });

                    productFlowPane.getChildren().add(card);

                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Unable to load ProductCard!");
                }
            }
        });
    }

    private void openAuctionDetail(Auction auction) {
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/SellProductInfo.fxml"));
            Parent root = loader.load();

            SellProductInfoController controller = loader.getController();
            controller.initData(auction);

            Stage sellProductInfoStage = new Stage();
            sellProductInfoStage.setTitle("Auction Detail");
            sellProductInfoStage.setResizable(false);
            sellProductInfoStage.initModality(Modality.APPLICATION_MODAL);
            sellProductInfoStage.initStyle(StageStyle.DECORATED);

            sellProductInfoStage.centerOnScreen();
            sellProductInfoStage.setScene(new Scene(root));
            sellProductInfoStage.setOnCloseRequest(event -> {
                controller.cleanUp();
            });
            sellProductInfoStage.show();

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onUpdateReceived(NetworkMessage msg) {
        String action = msg.getAction();
        if (action.equals("GET_PRODUCTS")){
            List<Auction> auctions = (List<Auction>) msg.getData();
            Platform.runLater(() -> updateProductList(auctions));
        }
    }
}

package auction_client.controllers;

import auction_client.interfaces.AuctionUpdateListener;
import auction_client.Network.ClientService;
import auction_client.interfaces.HandleCardClicked;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.AuctionDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
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

public class BidProductSceneController implements Initializable, AuctionUpdateListener, HandleCardClicked {
    @FXML
    private FlowPane productFlowPane;


    public void initialize(URL url, ResourceBundle resourceBundle) {
        ClientService.getInstance().addListener(this);
        ClientService.getInstance().sendMessage(new NetworkMessage("GET_PRODUCTS", null));
    }

    public void updateProductList(List<AuctionDTO> auctions) {
        Platform.runLater(() -> {
            // 1. Xóa các card cũ để tránh trùng lặp khi cập nhật
            productFlowPane.getChildren().clear();

            for (AuctionDTO auction : auctions) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/ProductCard.fxml"));
                    Parent card = loader.load();

                    ProductCardController cardController = loader.getController();
                    cardController.setData(auction, this::openAuctionDetail);
                    productFlowPane.getChildren().add(card);

                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Unable to load ProductCard!");
                }
            }
        });
    }

    public void openAuctionDetail(AuctionDTO auction) {
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/BidProductInfo.fxml"));
            Parent root = loader.load();

            BidProductInfoController controller = loader.getController();
            controller.initData(auction);

            Stage bidProductInfoStage = new Stage();
            bidProductInfoStage.setTitle("Auction Detail");
            bidProductInfoStage.initModality(Modality.APPLICATION_MODAL);

            bidProductInfoStage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(null);

            bidProductInfoStage.setScene(scene);
            bidProductInfoStage.centerOnScreen();

            bidProductInfoStage.setOnCloseRequest(event -> controller.cleanUp());
            bidProductInfoStage.show();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onUpdateReceived(NetworkMessage msg) {
        String action = msg.getAction();
        if (action.equals("GET_PRODUCTS")){
            List<AuctionDTO> auctions = (List<AuctionDTO>) msg.getData();
            Platform.runLater(() -> updateProductList(auctions));
        }
    }
}

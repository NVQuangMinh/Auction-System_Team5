package auction_client.controllers;

import auction_client.interfaces.AuctionUpdateListener;
import auction_client.interfaces.HandleCardClicked;
import auction_client.Network.ClientService;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.AuctionDTO;
import auction_shared.dto.ItemType;
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
import java.util.stream.Collectors;

public class FilteredProductSceneController implements Initializable, AuctionUpdateListener, HandleCardClicked {
    @FXML
    private FlowPane productFlowPane;

    private List<AuctionDTO> allAuctions = null;
    private ItemType targetType = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        String path = url.toExternalForm();
        if (path.contains("ArtScene")) {
            targetType = ItemType.ARTS;
        } else if (path.contains("ElectronicScene")) {
            targetType = ItemType.ELECTRONICS;
        } else if (path.contains("VehicleScene")) {
            targetType = ItemType.VEHICLES;
        }

        ClientService.getInstance().addListener(this);
        ClientService.getInstance().sendMessage(new NetworkMessage("GET_PRODUCTS", null));
    }

    private void updateProductList(List<AuctionDTO> auctions) {
        Platform.runLater(() -> {
            productFlowPane.getChildren().clear();

            for (AuctionDTO auction : auctions) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/ProductCard.fxml"));
                    Parent card = loader.load();

                    ProductCardController cardController = loader.getController();
                    cardController.setData(auction, this);
                    productFlowPane.getChildren().add(card);

                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Unable to load ProductCard!");
                }
            }
        });
    }

    public void openAuctionDetail(AuctionDTO auction) {
        try {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdateReceived(NetworkMessage msg) {
        String action = msg.getAction();
        if (action.equals("GET_PRODUCTS") || action.equals("UPDATE_BID")) {
            List<AuctionDTO> rawList = (List<AuctionDTO>) msg.getData();
            this.allAuctions = rawList;

            List<AuctionDTO> filtered = rawList.stream()
                    .filter(a -> a.getItem().getType() == targetType)
                    .collect(Collectors.toList());
            Platform.runLater(() -> updateProductList(filtered));
        }
    }
}

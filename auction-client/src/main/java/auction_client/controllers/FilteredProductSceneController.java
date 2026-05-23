package auction_client.controllers;

import auction_client.interfaces.AuctionUpdateListener;
import auction_client.interfaces.HandleCardClicked;
import auction_client.Network.ClientService;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.AuctionDTO;
import auction_shared.dto.ItemType;
import auction_shared.dto.ProductListResponse;
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
import java.util.ArrayList;
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

    public void cleanup() {
        ClientService.getInstance().removeListener(this);
    }

    private void updateProductList(List<AuctionDTO> auctions) {
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
            }
        }
    }

    public void openAuctionDetail(AuctionDTO auction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/BidProductInfo.fxml"));
            Parent root = loader.load();

            BidProductInfoController controller = loader.getController();
            controller.initData(auction);

            Stage stage = new Stage();
            stage.setTitle("Auction Detail");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(null);

            stage.setScene(scene);
            stage.centerOnScreen();
            stage.setOnCloseRequest(event -> controller.cleanUp());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdateReceived(NetworkMessage msg) {
        String action = msg.getAction();
        if (action.equals("GET_PRODUCTS")) {
            ProductListResponse response = (ProductListResponse) msg.getData();
            List<AuctionDTO> active = response.getActiveAuctions() != null ? response.getActiveAuctions() : new ArrayList<>();
            List<AuctionDTO> ended = response.getEndedSaledAuctions() != null ? response.getEndedSaledAuctions() : new ArrayList<>();

            List<AuctionDTO> merged = new ArrayList<>(active);
            merged.addAll(ended);
            this.allAuctions = merged;

            Platform.runLater(() -> {
                List<AuctionDTO> filtered = allAuctions.stream()
                        .filter(a -> a.getItem().getType() == targetType)
                        .collect(Collectors.toList());
                updateProductList(filtered);
            });
        } else if (action.equals("UPDATE_BID")) {
            List<AuctionDTO> rawList = (List<AuctionDTO>) msg.getData();
            this.allAuctions = rawList;
            Platform.runLater(() -> {
                List<AuctionDTO> filtered = allAuctions.stream()
                        .filter(a -> a.getItem().getType() == targetType)
                        .collect(Collectors.toList());
                updateProductList(filtered);
            });
        }
    }
}

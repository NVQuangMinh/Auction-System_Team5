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
import java.util.stream.Stream;

public class FilteredProductSceneController implements Initializable, AuctionUpdateListener, HandleCardClicked {
    @FXML
    private FlowPane productFlowPane;

    private List<AuctionDTO> activeAuctions = new ArrayList<>();
    private List<AuctionDTO> endedAuctions = new ArrayList<>();

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

    private List<AuctionDTO> buildFilteredList() {
        return Stream.concat(activeAuctions.stream(), endedAuctions.stream())
                .filter(a -> a.getItem().getType() == targetType)
                .collect(Collectors.toList());
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

            if(response.getActiveAuctions() != null){
                this.activeAuctions = new ArrayList<>(response.getActiveAuctions());
            } else {
                this.activeAuctions = new ArrayList<>();
            }

            if(response.getEndedSaledAuctions() != null){
                this.endedAuctions = new ArrayList<>(response.getEndedSaledAuctions());
            } else {
                this.activeAuctions = new ArrayList<>();
            }

            List<AuctionDTO> toShow = buildFilteredList();
            Platform.runLater(() -> updateProductList(toShow));

        } else if (action.equals("UPDATE_BID")) {
            // Fix bug: chỉ cập nhật phần ACTIVE, giữ nguyên ENDED
            List<AuctionDTO> rawList = (List<AuctionDTO>) msg.getData();
            this.activeAuctions = rawList != null ? new ArrayList<>(rawList) : new ArrayList<>();

            List<AuctionDTO> toShow = buildFilteredList();
            Platform.runLater(() -> updateProductList(toShow));

        } else if (action.equals("AUCTION_ENDED") || action.equals("AUCTION_SOLD")) {
            // chuyển auction từ active sang ended trong local state, không query DB
            AuctionDTO dto = (AuctionDTO) msg.getData();
            activeAuctions.removeIf(a -> a.getAuctionId().equals(dto.getAuctionId()));
            endedAuctions.add(0, dto);

            List<AuctionDTO> toShow = buildFilteredList();
            Platform.runLater(() -> updateProductList(toShow));

        } else if (action.equals("REMOVE_ITEM")) {
            // Admin hủy auction: xóa khỏi cả 2 list
            AuctionDTO removed = (AuctionDTO) msg.getData();
            activeAuctions.removeIf(a -> a.getAuctionId().equals(removed.getAuctionId()));
            endedAuctions.removeIf(a -> a.getAuctionId().equals(removed.getAuctionId()));

            List<AuctionDTO> toShow = buildFilteredList();
            Platform.runLater(() -> updateProductList(toShow));
        }
    }
}

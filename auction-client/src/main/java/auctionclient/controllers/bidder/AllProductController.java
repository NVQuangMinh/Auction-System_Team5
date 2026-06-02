package auctionclient.controllers.bidder;

import auctionclient.Network.ClientService;
import auctionclient.controllers.notification.UserPushUpNotificationController;
import auctionclient.interfaces.AuctionUpdateListener;
import auctionclient.interfaces.Cleanable;
import auctionclient.interfaces.HandleCardClicked;
import auctionshared.Network.NetworkMessage;
import auctionclient.models.ProductListManager;
import auctionshared.dto.AuctionDTO;
import auctionshared.dto.AuctionStatus;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AllProductController implements Initializable, AuctionUpdateListener, HandleCardClicked, Cleanable {
    @FXML
    private FlowPane productFlowPane;

    // Category filters
    @FXML
    private RadioButton allCategoryRadio;
    @FXML
    private RadioButton artsCategoryRadio;
    @FXML
    private RadioButton electronicsCategoryRadio;
    @FXML
    private RadioButton vehiclesCategoryRadio;
    @FXML
    private ToggleGroup categoryGroup;

    // Status filters
    @FXML
    private RadioButton activeStatusRadio;
    @FXML
    private RadioButton endedStatusRadio;
    @FXML
    private ToggleGroup statusGroup;



    private ProductListManager listManager = new ProductListManager();

    private String currentCategoryFilter = "ALL";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        allCategoryRadio.setSelected(true);
        activeStatusRadio.setSelected(true);


        ClientService.getInstance().addListener(this);
        ClientService.getInstance().sendMessage(new NetworkMessage("GET_ACTIVE_PRODUCTS", null));
    }

    @Override
    public void cleanup() {
        ClientService.getInstance().removeListener(this);
    }

    @FXML
    private void onCategorySelected() {
        currentCategoryFilter = resolveCategoryFilter();
        refreshCurrentView();
    }

    @FXML
    private void onStatusSelected() {
        refreshCurrentView();
    }

    private String resolveCategoryFilter() {
        if (allCategoryRadio.isSelected()) {
            return "ALL";
        }
        if (artsCategoryRadio.isSelected()) {
            return "ARTS";
        }
        if (electronicsCategoryRadio.isSelected()) {
            return "ELECTRONICS";
        }
        if (vehiclesCategoryRadio.isSelected()) {
            return "VEHICLES";
        }
        return "ALL";
    }

    private void refreshCurrentView() {
        if (activeStatusRadio.isSelected()) {
            displayActivePage();
        } else if (endedStatusRadio.isSelected()) {
            requestEndedPage();
        }
    }

    private void displayActivePage() {
        List<AuctionDTO> filtered = listManager.filterCategory(listManager.getActiveAuctions(), currentCategoryFilter);
        updateProductList(filtered);
    }



    private void requestEndedPage() {
        ClientService.getInstance().sendMessage(
                new NetworkMessage("GET_ENDED_PRODUCTS", currentCategoryFilter));
    }

    private void updateProductList(List<AuctionDTO> auctions) {
        productFlowPane.getChildren().clear();
        for (AuctionDTO auction : auctions) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/auctionclient/ProductCard.fxml"));
                Parent card = loader.load();
                ProductCardController cardController = loader.getController();
                cardController.setData(auction, this);
                productFlowPane.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void openAuctionDetail(AuctionDTO auctionDTO) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auctionclient/BidProductInfo.fxml"));
            Parent root = loader.load();

            BidProductInfoController controller = loader.getController();
            controller.initData(auctionDTO);

            Stage stage = new Stage();
            stage.setTitle("Auction Detail");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(null);

            stage.setScene(scene);
            stage.centerOnScreen();
            stage.setOnCloseRequest(event -> controller.cleanup());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdateReceived(NetworkMessage msg) {
        String action = msg.getAction();

        if (action.equals("GET_ACTIVE_PRODUCTS")) {
            List<AuctionDTO> response = (List<AuctionDTO>) msg.getData();
            Platform.runLater(() -> handleGetProductsResponse(response));

        } else if (action.equals("GET_ENDED_PRODUCTS")) {
            List<AuctionDTO> response = (List<AuctionDTO>) msg.getData();
            Platform.runLater(() -> handleGetEndedProductsResponse(response));

        } else if (action.equals("UPDATE_BID")) {
            List<AuctionDTO> allDTOs = (List<AuctionDTO>) msg.getData();
            Platform.runLater(() -> handleUpdateBid(allDTOs));

        } else if (action.equals("REMOVE_ITEM")) {
            AuctionDTO removed = (AuctionDTO) msg.getData();
            Platform.runLater(() -> {
                // Xóa khỏi cả 2 list khi bị banned
                listManager.getActiveAuctions().removeIf(a -> a.getItem().getId().equals(removed.getItem().getId()));
                listManager.getEndedSaledAuctions().removeIf(a -> a.getItem().getId().equals(removed.getItem().getId()));
                refreshCurrentView();
            });
        }
    }

    private void handleGetProductsResponse(List<AuctionDTO> response) {
        listManager.setActiveAuctions(response);

        refreshCurrentView();
    }

    private void handleGetEndedProductsResponse(List<AuctionDTO> response) {
        listManager.setEndedSaledAuctions(response);
        updateProductList(listManager.getEndedSaledAuctions());
    }

    private void handleUpdateBid(List<AuctionDTO> allDTOs) {
        listManager.setActiveAuctions(allDTOs.stream()
                .filter(a -> a.getStatus() == AuctionStatus.ACTIVE)
                .collect(Collectors.toList()));

        if (activeStatusRadio.isSelected()) {
            displayActivePage();
        }
    }
}

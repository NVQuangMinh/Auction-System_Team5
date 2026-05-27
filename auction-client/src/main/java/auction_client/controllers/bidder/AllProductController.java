package auction_client.controllers.bidder;

import auction_client.Network.ClientService;
import auction_client.controllers.notification.UserPushUpNotificationController;
import auction_client.interfaces.AuctionUpdateListener;
import auction_client.interfaces.Cleanable;
import auction_client.interfaces.HandleCardClicked;
import auction_shared.Network.NetworkMessage;
import auction_client.models.ProductListManager;
import auction_shared.dto.AuctionDTO;
import auction_shared.dto.AuctionStatus;
import auction_shared.dto.EndedProductsRequest;
import auction_shared.dto.ItemType;
import auction_shared.dto.ProductListResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AllProductController implements Initializable, AuctionUpdateListener, HandleCardClicked, Cleanable {
    private static final int PAGE_SIZE = 12;

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

    // Pagination
    @FXML
    private Label pageInfoLabel;
    @FXML
    private Label prevButton;
    @FXML
    private Label nextButton;

    private ProductListManager listManager = new ProductListManager();

    // Trạng thái phân trang hiện tại
    private int endedPage = 0;
    private String currentCategoryFilter = "ALL";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        allCategoryRadio.setSelected(true);
        activeStatusRadio.setSelected(true);

        prevButton.setOnMouseClicked(e -> handlePrevPage());
        nextButton.setOnMouseClicked(e -> handleNextPage());

        ClientService.getInstance().addListener(this);
        ClientService.getInstance().sendMessage(new NetworkMessage("GET_PRODUCTS", null));
    }

    @Override
    public void cleanup() {
        ClientService.getInstance().removeListener(this);
    }

    @FXML
    private void onCategorySelected() {
        endedPage = 0;
        currentCategoryFilter = resolveCategoryFilter();
        refreshCurrentView();
    }

    @FXML
    private void onStatusSelected() {
        endedPage = 0;
        refreshCurrentView();
    }

    private String resolveCategoryFilter() {
        if (allCategoryRadio.isSelected())
            return "ALL";
        if (artsCategoryRadio.isSelected())
            return "ARTS";
        if (electronicsCategoryRadio.isSelected())
            return "ELECTRONICS";
        if (vehiclesCategoryRadio.isSelected())
            return "VEHICLES";
        return "ALL";
    }

    private void refreshCurrentView() {
        if (activeStatusRadio.isSelected()) {
            // Chỉ hiển thị ACTIVE
            displayActivePage();
        } else if (endedStatusRadio.isSelected()) {
            // Chỉ hiển thị ENDED/SOLD (từ DB)
            requestEndedPage(endedPage);
        }
    }

    private void displayActivePage() {
        List<AuctionDTO> filtered = listManager.filterCategory(listManager.getActiveAuctions(), currentCategoryFilter);
        List<AuctionDTO> pageItems = listManager.paginate(filtered, 0, PAGE_SIZE);

        updateProductList(pageItems);
        updatePageInfo(1, 1);
        updateNavButtons(0, 1);
    }

    private void handlePrevPage() {
        if (endedStatusRadio.isSelected() && endedPage > 0) {
            endedPage--;
            requestEndedPage(endedPage);
        }
    }

    private void handleNextPage() {
        if (!endedStatusRadio.isSelected())
            return;

        int totalEndedPages = (int) Math.ceil((double) listManager.getEndedTotalCount() / PAGE_SIZE);
        if (endedPage + 1 < totalEndedPages) {
            endedPage++;
            requestEndedPage(endedPage);
        }
    }

    @FXML
    private void onRefreshClicked() {
        if (activeStatusRadio.isSelected()) {
            ClientService.getInstance().sendMessage(new NetworkMessage("GET_PRODUCTS", null));
        } else {
            endedPage = 0;
            requestEndedPage(endedPage);
        }
    }

    private void requestEndedPage(int page) {
        ClientService.getInstance().sendMessage(
                new NetworkMessage("GET_ENDED_PRODUCTS",
                        new EndedProductsRequest(currentCategoryFilter, page, PAGE_SIZE)));
    }

    private int getTotalEndedPages() {
        if (listManager.getEndedTotalCount() == 0)
            return 1;
        return (int) Math.ceil((double) listManager.getEndedTotalCount() / PAGE_SIZE);
    }

    private void updatePageInfo(int current, int total) {
        pageInfoLabel.setText(current + " / " + total);
    }

    private void updateNavButtons(int current, int total) {
        prevButton.setDisable(true);
        nextButton.setDisable(true);
        if (endedStatusRadio.isSelected() && listManager.getEndedTotalCount() > 0) {
            int totalPages = getTotalEndedPages();
            prevButton.setDisable(endedPage == 0);
            nextButton.setDisable(endedPage >= totalPages - 1);
        }

        String disabledStyle = "-fx-text-fill: #aaa; -fx-opacity: 0.4;";
        String enabledStyle = "-fx-text-fill: #138eff; -fx-opacity: 1.0; -fx-cursor: hand;";
        prevButton.setStyle(prevButton.isDisable() ? disabledStyle : enabledStyle);
        nextButton.setStyle(nextButton.isDisable() ? disabledStyle : enabledStyle);
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

    @Override
    public void openAuctionDetail(AuctionDTO auctionDTO) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/BidProductInfo.fxml"));
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

        if (action.equals("GET_PRODUCTS")) {
            ProductListResponse response = (ProductListResponse) msg.getData();
            Platform.runLater(() -> handleGetProductsResponse(response));

        } else if (action.equals("GET_ENDED_PRODUCTS")) {
            ProductListResponse response = (ProductListResponse) msg.getData();
            Platform.runLater(() -> handleGetEndedProductsResponse(response));

        } else if (action.equals("UPDATE_BID")) {
            List<AuctionDTO> allDTOs = (List<AuctionDTO>) msg.getData();
            Platform.runLater(() -> handleUpdateBid(allDTOs));

        } else if (action.equals("AUCTION_ENDED") || action.equals("AUCTION_SOLD")) {
            AuctionDTO dto = (AuctionDTO) msg.getData();
            Platform.runLater(() -> {
                String itemName = dto.getItem().getName();
                UserPushUpNotificationController.showNotification(
                        "Phiên đấu giá kết thúc: " + itemName, "INFO");
            });

        } else if (action.equals("REMOVE_ITEM")) {
            AuctionDTO removed = (AuctionDTO) msg.getData();
            Platform.runLater(() -> {
                // Xóa khỏi cả 2 list khi bị banned
                listManager.getActiveAuctions().removeIf(a -> a.getItem().getId().equals(removed.getItem().getId()));
                boolean wasInEnded = listManager.getEndedSaledAuctions().removeIf(
                        a -> a.getItem().getId().equals(removed.getItem().getId()));
                if (wasInEnded) {
                    listManager.setEndedTotalCount(listManager.getEndedTotalCount() - 1);
                }
                refreshCurrentView();
            });
        }
    }

    private void handleGetProductsResponse(ProductListResponse response) {
        listManager.setActiveAuctions(response.getActiveAuctions());
        listManager.setEndedSaledAuctions(response.getEndedSaledAuctions());
        listManager.setEndedTotalCount(response.getEndedTotalCount());

        refreshCurrentView();
    }

    private void handleGetEndedProductsResponse(ProductListResponse response) {
        listManager.setEndedSaledAuctions(response.getEndedSaledAuctions());
        listManager.setEndedTotalCount(response.getEndedTotalCount());

        // Hiển thị trang ENDED/SOLD hiện tại
        updateProductList(listManager.getEndedSaledAuctions());
        updatePageInfo(endedPage + 1, getTotalEndedPages());
        updateNavButtons(endedPage, getTotalEndedPages());
    }

    private void handleUpdateBid(List<AuctionDTO> allDTOs) {
        // UPDATE_BID từ server chứa tất cả ACTIVE từ RAM
        // Cập nhật danh sách ACTIVE
        listManager.setActiveAuctions(allDTOs.stream()
                .filter(a -> a.getStatus() == AuctionStatus.ACTIVE)
                .collect(Collectors.toList()));

        // Refresh view hiện tại
        refreshCurrentView();
    }
}

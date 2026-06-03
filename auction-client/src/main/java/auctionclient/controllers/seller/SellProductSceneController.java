package auctionclient.controllers.seller;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import auctionclient.Network.ClientService;
import auctionclient.UserSession;
import auctionclient.interfaces.AuctionUpdateListener;
import auctionclient.interfaces.Cleanable;
import auctionclient.interfaces.HandleCardClicked;
import auctionclient.controllers.bidder.ProductCardController;
import auctionclient.models.ProductListManager;
import auctionshared.Network.NetworkMessage;
import auctionshared.dto.AuctionDTO;
import auctionshared.dto.AuctionStatus;
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

/**
 * Controller cho màn hình "Sản phẩm của tôi" (Seller).
 *
 * Hiển thị 2 danh sách song song:
 * - sellingFlowPane : sản phẩm ACTIVE (đang bán) — lấy từ RAM qua GET_MY_LIST
 * - soldFlowPane    : sản phẩm ENDED/SOLD (đã bán) — lấy từ DB qua GET_MY_ENDED_LIST
 *
 * Tái sử dụng ProductListManager theo đúng pattern của AllProductController.
 */
public class SellProductSceneController implements Initializable, AuctionUpdateListener, HandleCardClicked, Cleanable {

    @FXML public AnchorPane overlayPane;
    @FXML private FlowPane sellingFlowPane;   // Cột trái: đang bán
    @FXML private FlowPane soldFlowPane;      // Cột phải: đã bán

    // Tái sử dụng ProductListManager — đồng nhất với AllProductController
    private final ProductListManager listManager = new ProductListManager();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ClientService.getInstance().addListener(this);
        String username = UserSession.getInstance().getUsername();
        // Lấy ACTIVE từ RAM
        ClientService.getInstance().sendMessage(new NetworkMessage("GET_MY_LIST", username));
        // Lấy ENDED/SOLD từ DB
        ClientService.getInstance().sendMessage(new NetworkMessage("GET_MY_ENDED_LIST", username));
    }

    @Override
    public void cleanup() {
        ClientService.getInstance().removeListener(this);
    }

    @FXML
    public void handleUserAddItem(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/auctionclient/ProductInfoSubmission.fxml"));
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
        stage.setTitle("Đăng bán sản phẩm");
        overlayPane.setVisible(true);
        stage.show();
    }

    @Override
    public void onUpdateReceived(NetworkMessage msg) {
        String username = UserSession.getInstance().getUsername();
        switch (msg.getAction()) {
            case "GET_MY_LIST" -> {
                List<AuctionDTO> active = (List<AuctionDTO>) msg.getData();
                listManager.setActiveAuctions(active);
                Platform.runLater(() -> renderFlowPane(sellingFlowPane, listManager.getActiveAuctions()));
            }
            case "GET_MY_ENDED_LIST" -> {
                List<AuctionDTO> ended = (List<AuctionDTO>) msg.getData();
                listManager.setEndedSaledAuctions(ended);
                Platform.runLater(() -> renderFlowPane(soldFlowPane, listManager.getEndedSaledAuctions()));
            }
            case "UPDATE_BID" -> {
                // Server broadcast toàn bộ ACTIVE — lọc lấy của user hiện tại
                List<AuctionDTO> mine = ((List<AuctionDTO>) msg.getData()).stream()
                        .filter(a -> a.getStatus() == AuctionStatus.ACTIVE
                                  && username.equals(a.getItem().getOwner().getUsername()))
                        .collect(Collectors.toList());
                listManager.setActiveAuctions(mine);
                Platform.runLater(() -> renderFlowPane(sellingFlowPane, listManager.getActiveAuctions()));
            }
            case "AUCTION_SOLD", "AUCTION_ENDED" -> {
                // Phiên vừa kết thúc: refresh cột Đã Bán từ DB
                ClientService.getInstance().sendMessage(
                        new NetworkMessage("GET_MY_ENDED_LIST", username));
            }
        }
    }

    /**
     * Render danh sách AuctionDTO vào FlowPane chỉ định.
     * Xoá nội dung cũ rồi vẽ lại từ đầu.
     */
    private void renderFlowPane(FlowPane pane, List<AuctionDTO> auctions) {
        pane.getChildren().clear();
        for (AuctionDTO auction : auctions) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/auctionclient/ProductCard.fxml"));
                Parent card = loader.load();
                ProductCardController cardController = loader.getController();
                cardController.setData(auction, this::openAuctionDetail);
                pane.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void openAuctionDetail(AuctionDTO auction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auctionclient/SellProductInfo.fxml"));
            Parent root = loader.load();
            SellProductInfoController controller = loader.getController();
            controller.initData(auction);
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
}

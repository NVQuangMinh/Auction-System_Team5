package auction_client.controllers;

import auction_client.Network.ClientService;
import auction_shared.Network.NetworkMessage;
import auction_shared.entities.Auction;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.FlowPane;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class BidProductSceneController implements Initializable {
    @FXML
    private FlowPane productFlowPane;


    public void initialize(URL url, ResourceBundle resourceBundle) {
        ClientService.getInstance().setAuctionListCallback(this::updateProductList);
        ClientService.getInstance().sendMessage(new NetworkMessage("GET_PRODUCTS", null));
    }

    public void updateProductList(List<Auction> auctions) {
        // Bắt buộc dùng Platform.runLater vì dữ liệu đến từ luồng Socket
        Platform.runLater(() -> {
            // 1. Xóa các card cũ để tránh trùng lặp khi cập nhật
            productFlowPane.getChildren().clear();

            for (Auction auction : auctions) {
                try {
                    // 2. Load FXML của tấm thẻ sản phẩm
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/ProductCard.fxml"));
                    Parent card = loader.load();

                    // 3. Lấy controller của Card để đổ dữ liệu (Tên, giá, ảnh...)
                    ProductCardController cardController = loader.getController();
                    cardController.setData(auction);

                    // 4. Chỉ cần "add" vào là xong, FlowPane tự xếp hàng
                    productFlowPane.getChildren().add(card);

                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Không thể load ProductCard!");
                }
            }
        });
    }
}

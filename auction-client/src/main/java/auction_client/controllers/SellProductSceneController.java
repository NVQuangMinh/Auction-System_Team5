package auction_client.controllers;

import auction_client.Network.ClientService;
import auction_client.product.LoadProductInfoHandler;
import auction_shared.Network.NetworkMessage;
import auction_shared.entities.Auction;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class SellProductSceneController extends LoadProductInfoHandler implements Initializable {
    @FXML
    public AnchorPane overlayPane;
    @FXML
    private WebMenuBarController menuBarController;

    public WebMenuBarController getMenuBarController() {
        return menuBarController;
    }

    @FXML
    public void handleUserAddItem(ActionEvent event) throws IOException{
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/auction_client/ProductInfoSubmission.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        Stage owner = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.initOwner(owner);
        stage.setOnHiding(e -> {
            if (overlayPane != null) overlayPane.setVisible(false);
        });
        stage.setScene(scene);
        stage.setTitle("Add New Product");
        if (overlayPane != null) overlayPane.setVisible(true);
        stage.show();
    }
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ClientService.getInstance().setAuctionListCallback(this::updateProductList);
        ClientService.getInstance().sendMessage(new NetworkMessage("GET_PRODUCTS", null));
    }
    
    public void loadSellProductInfo(Auction auction){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/SellProductInfo.fxml"));
            Parent productInfo = loader.load();
            SellProductInfoController controller = loader.getController();
            controller.setData(auction);
            
            Scene productInfoScene = new Scene(productInfo);
            productInfoScene.setFill(Color.TRANSPARENT);
            
            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            
            Stage owner = (Stage) productFlowPane.getScene().getWindow();
            stage.initOwner(owner);
            
            stage.setOnHiding(e -> {
                if (overlayPane != null) overlayPane.setVisible(false);
            });
            
            stage.setScene(productInfoScene);
            stage.setTitle("sellProductInfo");
            
            if (overlayPane != null) overlayPane.setVisible(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateProductList(List<Auction> auctions) {
        Platform.runLater(() -> {
            displayProduct(auctions, this::loadSellProductInfo);
        });
    }
}

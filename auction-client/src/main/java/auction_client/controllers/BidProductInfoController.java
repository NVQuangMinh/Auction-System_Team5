package auction_client.controllers;

import auction_client.product.LoadProductInfoHandler;
import auction_shared.entities.Auction;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;


public class BidProductInfoController {
    @FXML
    private CheckBox autoBidCheck;
    @FXML
    private HBox bidContainer;
    @FXML
    private HBox autoBidContainer;
    @FXML
    protected Label productName;
    @FXML
    protected Label productDescription;
    @FXML
    protected Label productCurrentPrice;
    @FXML
    protected Label productBuyOutPrice;
    @FXML
    protected Label productTickRate;
    @FXML
    protected Label productBidTime;

    @FXML
    public void initialize() {
        bidContainer.managedProperty().bind(bidContainer.visibleProperty());
        autoBidContainer.managedProperty().bind(autoBidContainer.visibleProperty());
        autoBidContainer.visibleProperty().bind(autoBidCheck.selectedProperty());
        bidContainer.visibleProperty().bind(autoBidCheck.selectedProperty().not());
    }
    public void setData(Auction auction) {
        productName.setText(auction.getItem().getName());
        productDescription.setText(auction.getItem().getProductDescription());
        productCurrentPrice.setText(String.valueOf(auction.getCurrentHighestBid()));
        productBuyOutPrice.setText(String.valueOf(auction.getItem().getBuyOutPrice()));
        productTickRate.setText((String.valueOf(auction.getItem().getProductTickRate())));
    }
    @FXML
    private void switchToUserProductList(MouseEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}


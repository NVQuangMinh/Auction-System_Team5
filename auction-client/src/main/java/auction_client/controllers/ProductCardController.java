package auction_client.controllers;

import auction_client.Network.ClientService;
import auction_client.product.LoadProductInfo;
import auction_shared.Network.NetworkMessage;
import auction_shared.entities.Auction;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ProductCardController {
    @FXML
    protected Label itemName;
    @FXML
    protected Label itemState;
    @FXML
    protected Label currentPrice;
    @FXML
    protected Label buyOutPrice;
    @FXML
    protected Label productDescription;
    private LoadProductInfo loadProductInfo = null;
    Auction auction = null;

    public void setData(Auction auction, LoadProductInfo loadProductInfo) {
        this.auction = auction;
        this.loadProductInfo = loadProductInfo;
        itemName.setText(auction.getItem().getName());
        itemState.setText(auction.getState());
        currentPrice.setText(String.valueOf(auction.getCurrentHighestBid()));
        buyOutPrice.setText(String.valueOf(auction.getItem().getBuyOutPrice()));
        productDescription.setText(auction.getItem().getProductDescription());
    }
    @FXML
    public void handleCardClick() {
        if (loadProductInfo != null) {
            loadProductInfo.onProductClicked(this.auction);
        }
    }

    @FXML
    public void buyOut(ActionEvent event){
        ClientService.getInstance().sendMessage(new NetworkMessage("BUY_OUT", auction));
    }
}

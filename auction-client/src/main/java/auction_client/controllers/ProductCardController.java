package auction_client.controllers;

import auction_client.Network.ClientService;
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
    protected Label description;

    Auction auction = null;

    public void setData(Auction auction) {
        this.auction = auction;
        itemName.setText(auction.getItem().getName());
        itemState.setText(auction.getState());
        currentPrice.setText(String.valueOf(auction.getCurrentHighestBid()));
        description.setText(auction.getItem().getDescription());
    }

    @FXML
    public void buyOut(ActionEvent event){
        ClientService.getInstance().sendMessage(new NetworkMessage("BUY_OUT", auction));

    }
}

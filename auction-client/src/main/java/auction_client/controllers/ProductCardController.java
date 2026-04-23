package auction_client.controllers;

import auction_shared.entities.Auction;
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

    public void setData(Auction auction) {
        itemName.setText(auction.getItem().getName());
        itemState.setText(auction.getState());
        currentPrice.setText(String.valueOf(auction.getCurrentHighestBid()));
        description.setText(auction.getItem().getDescription());
    }
}

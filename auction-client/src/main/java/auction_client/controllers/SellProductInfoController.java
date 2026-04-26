package auction_client.controllers;

import auction_client.AuctionUpdateListener;
import auction_client.Network.ClientService;
import auction_shared.Network.NetworkMessage;
import auction_shared.entities.Auction;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class SellProductInfoController implements AuctionUpdateListener {
    @FXML
    Label itemName;
    @FXML
    Label description;
    @FXML
    Label currentPrice;
    @FXML
    Label buyOut;
    @FXML
    Label tickRate;
    @FXML
    Label timeLeft;
    Auction auction = null;


    public void initData(Auction auction){
        this.auction = auction;
        updateData();
        ClientService.getInstance().addListener(this);

    }

    public void updateData(){
        Platform.runLater(() ->{
            currentPrice.setText(String.valueOf(auction.getCurrentHighestBid()));
            itemName.setText(auction.getItem().getName());
            description.setText(auction.getItem().getDescription());
        });
    }

    public void onUpdateReceived(NetworkMessage msg) {
        String action = msg.getAction();
        if (action.equals("UPDATE_BID")){
            Auction updatedAuction = (Auction) msg.getData();
            if (updatedAuction.getItem().getId().equals(auction.getItem().getId())){
                this.auction = updatedAuction;
                updateData();
            }

        }
    }

    public void cleanUp(){
        ClientService.getInstance().removeListener(this);
    }
}

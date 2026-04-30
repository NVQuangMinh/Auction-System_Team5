package auction_client.controllers;

import auction_client.Network.ClientService;
import auction_client.UserSession;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.AuctionDTO;
import auction_shared.dto.BidTransactionDTO;
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

    AuctionDTO auction = null;

    public void setData(AuctionDTO auction) {
        this.auction = auction;
        itemName.setText(auction.getItem().getName());
        itemState.setText("ON-GOING");
        currentPrice.setText(String.valueOf(auction.getCurrentHighestBid()));
        description.setText(auction.getItem().getDescription());
    }

    @FXML
    public void buyOut(){
        //null == BidTransaction nhe anh em!
        BidTransactionDTO transaction = new BidTransactionDTO(auction, UserSession.getInstance().getUser(), 0);
        ClientService.getInstance().sendMessage(new NetworkMessage("BUY_OUT", transaction));

    }
}

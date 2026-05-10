package auction_client.controllers;

import auction_client.Network.ClientService;
import auction_client.UserSession;
import auction_client.interfaces.HandleCardClicked;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.AuctionDTO;
import auction_shared.dto.BidTransactionDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;


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
    protected Label description;

    private AuctionDTO auction = null;
    private HandleCardClicked cardClickedListener = null;

    public void setData(AuctionDTO auction, HandleCardClicked openAuctionDetail) {
        this.auction = auction;
        this.cardClickedListener = openAuctionDetail;
        itemName.setText(auction.getItem().getName());
        itemState.setText("ON-GOING");
        currentPrice.setText(String.valueOf(auction.getCurrentHighestBid()));
        buyOutPrice.setText(String.valueOf(auction.getBuyOutPrice()));
        description.setText(auction.getItem().getDescription());
    }

    public void handleCardClick(){
        this.cardClickedListener.openAuctionDetail(this.auction);
    }

    @FXML
    public void buyOut(){
        BidTransactionDTO transaction = new BidTransactionDTO(auction, UserSession.getInstance().getUser(), auction.getBuyOutPrice());
        ClientService.getInstance().sendMessage(new NetworkMessage("BUY_OUT", transaction));

    }
}

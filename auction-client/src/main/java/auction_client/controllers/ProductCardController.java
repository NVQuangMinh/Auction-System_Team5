package auction_client.controllers;

import auction_client.Network.ClientService;
import auction_client.UserSession;
import auction_client.interfaces.HandleCardClicked;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.AuctionDTO;
import auction_shared.dto.BidTransactionDTO;
import auction_shared.dto.ItemType;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

import java.text.DecimalFormat;
import java.time.LocalDateTime;

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
    @FXML
    protected FontIcon productIcon;

    private AuctionDTO auction = null;
    private HandleCardClicked cardClickedListener = null;

    public void setData(AuctionDTO auction, HandleCardClicked openAuctionDetail) {
        DecimalFormat df = new DecimalFormat("#,###.###");
        this.auction = auction;
        this.cardClickedListener = openAuctionDetail;
        itemName.setText(auction.getItem().getName());
        itemState.setText(String.valueOf(auction.getStatus()));
        currentPrice.setText("$" + df.format(auction.getCurrentHighestBid()));
        buyOutPrice.setText("$" + df.format(auction.getBuyOutPrice()));
        description.setText(auction.getItem().getDescription());
    }

    public void handleCardClick() {
        this.cardClickedListener.openAuctionDetail(this.auction);
    }

    @FXML
    public void buyOut() {
        BidTransactionDTO transaction = new BidTransactionDTO(auction, UserSession.getInstance().getUser(),
                auction.getBuyOutPrice(), LocalDateTime.now());
        ClientService.getInstance().sendMessage(new NetworkMessage("BUY_OUT", transaction));

    }
}

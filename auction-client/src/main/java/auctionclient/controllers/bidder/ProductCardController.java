package auctionclient.controllers.bidder;

import auctionclient.Network.ClientService;
import auctionclient.UserSession;
import auctionclient.interfaces.HandleCardClicked;
import auctionshared.Network.NetworkMessage;
import auctionshared.dto.AuctionDTO;
import auctionshared.dto.BidTransactionDTO;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class ProductCardController implements Initializable {
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
    protected Label typeSpecificLabel;
    @FXML
    protected ImageView itemIMage;

    private AuctionDTO auction = null;
    private HandleCardClicked cardClickedListener = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        itemIMage.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> {
            clip.setWidth(newVal.getWidth());
            clip.setHeight(newVal.getHeight());
        });
        itemIMage.setClip(clip);
    }

    public void setData(AuctionDTO auction, HandleCardClicked openAuctionDetail) {
        this.auction = auction;
        this.cardClickedListener = openAuctionDetail;
        itemName.setText(auction.getItem().getName());
        itemState.setText(auction.getStatus().name());

        currentPrice.setText(formatPrice(auction.getCurrentHighestBid()));
        buyOutPrice.setText(formatPrice(auction.getBuyOutPrice()));
        description.setText(auction.getItem().getDescription());

        String itemTypeStr = auction.getItem().getType().toString().toLowerCase();
        String imagePath = "/auctionclient/images/" + itemTypeStr + ".jpg";
        try {
            Image image = new Image(getClass().getResourceAsStream(imagePath));
            if (!image.isError()) {
                itemIMage.setImage(image);
            }
        } catch (NullPointerException e) {
            System.out.println("Cannot find image at: " + imagePath);
        }

        String attr = auction.getItem().getTypeSpecificAttribute();
        if (attr != null && !attr.isBlank()) {
            String label = auction.getItem().getTypeAttributeLabel() + ": " + attr;
            typeSpecificLabel.setText(label);
            typeSpecificLabel.setVisible(true);
            typeSpecificLabel.setManaged(true);
        } else {
            typeSpecificLabel.setText("");
            typeSpecificLabel.setVisible(false);
            typeSpecificLabel.setManaged(false);
        }
    }

    private String formatPrice(double value) {
        if (value == (long) value) {
            return String.format("$%,d", (long) value);
        } else {
            return String.format("$%,.2f", value);
        }
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

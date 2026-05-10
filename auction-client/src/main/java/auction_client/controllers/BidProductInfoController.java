package auction_client.controllers;

import auction_client.Network.ClientService;
import auction_client.UserSession;
import auction_client.interfaces.AuctionUpdateListener;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.AuctionDTO;
import auction_shared.dto.BidTransactionDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class BidProductInfoController implements Initializable, AuctionUpdateListener {
    @FXML
    Label error;
    @FXML
    private CheckBox autoBidCheck;
    @FXML
    private HBox bidContainer;
    @FXML
    private HBox autoBidContainer;
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
    Label timeLeft; // I still don't know what to do with this shit;

    @FXML
    TextField bidAmount;

    AuctionDTO auction = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ClientService.getInstance().addListener(this);
        bidContainer.managedProperty().bind(bidContainer.visibleProperty());
        autoBidContainer.managedProperty().bind(autoBidContainer.visibleProperty());
        autoBidContainer.visibleProperty().bind(autoBidCheck.selectedProperty());
        bidContainer.visibleProperty().bind(autoBidCheck.selectedProperty().not());
        ///  make the error disappear
        error.setOpacity(0.0);
        error.setManaged(false);
        error.setVisible(false);

    }

    public void updateData(){
        Platform.runLater(() ->{
            currentPrice.setText(String.valueOf(auction.getCurrentHighestBid()));
            itemName.setText(auction.getItem().getName());
            buyOut.setText(String.valueOf(auction.getBuyOutPrice()));
            tickRate.setText(String.valueOf(auction.getTickSize()));
            description.setText(auction.getItem().getDescription());
        });
    }

    public void onUpdateReceived(NetworkMessage msg) {
        String action = msg.getAction();
        if (action.equals("BID_SUCCESS")){
            this.auction = (AuctionDTO) msg.getData();
            updateData();
        }
        else if (action.equals("BUYOUT_SUCCESS")){
            cleanUp();
            switchToUserProductList();
        }
    }

    public void initData(AuctionDTO auction) {
        this.auction = auction;
        updateData();
    }
    @FXML
    private void switchToUserProductList() {
        Stage stage = (Stage) tickRate.getScene().getWindow();
        cleanUp();
        stage.close();
    }

    @FXML
    public void placeBidRequest(){
        double amount = Double.parseDouble(bidAmount.getText());
        if (amount >= auction.getBuyOutPrice()){
            BidTransactionDTO transaction = new BidTransactionDTO(auction, UserSession.getInstance().getUser(), auction.getBuyOutPrice());
            ClientService.getInstance().sendMessage(new NetworkMessage("BUY_OUT", transaction));
            // change the label (notify) -> transparent
            error.setOpacity(0.0);
            error.setManaged(false);
            error.setVisible(false);
        }
        else if ((amount - auction.getCurrentHighestBid()) % auction.getTickSize() == 0){
            BidTransactionDTO transaction = new BidTransactionDTO(auction, UserSession.getInstance().getUser(),amount);
            ClientService.getInstance().sendMessage(new NetworkMessage("PLACE_BID", transaction));
            // change the label (notify) -> transparent
            error.setOpacity(0.0);
            error.setManaged(false);
            error.setVisible(false);
        }
        else{
            // notify invalid bidAmount
            error.setVisible(true);
            error.setManaged(true);
            error.setOpacity(1.0);
            error.setText("Invalid Bid Amount!");
            error.setTextFill(Color.RED);
        }
    }

    public void cleanUp(){
        ClientService.getInstance().removeListener(this);
    }
}

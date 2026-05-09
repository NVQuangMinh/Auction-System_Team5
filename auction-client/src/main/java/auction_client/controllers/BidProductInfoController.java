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
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class BidProductInfoController implements Initializable, AuctionUpdateListener {
    @FXML
    private FlowPane productFlowPane;
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
        ClientService.getInstance().sendMessage(new NetworkMessage("GET_PRODUCTS", null));
        bidContainer.managedProperty().bind(bidContainer.visibleProperty());
        autoBidContainer.managedProperty().bind(autoBidContainer.visibleProperty());
        autoBidContainer.visibleProperty().bind(autoBidCheck.selectedProperty());
        bidContainer.visibleProperty().bind(autoBidCheck.selectedProperty().not());

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
        if (action.equals("UPDATE_BID")){
            AuctionDTO updatedAuction = (AuctionDTO) msg.getData();
            if (updatedAuction.getItem().getId().equals(auction.getItem().getId())){
                this.auction = updatedAuction;
                updateData();
            }
        }
    }

    public void initData(AuctionDTO auction) {
        itemName.setText(auction.getItem().getName());
        description.setText(auction.getItem().getDescription());
        currentPrice.setText(String.valueOf(auction.getCurrentHighestBid()));
        buyOut.setText(String.valueOf(auction.getBuyOutPrice()));
        tickRate.setText((String.valueOf(auction.getTickSize())));
    }
    @FXML
    private void switchToUserProductList(MouseEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    public void placeBidRequest(){
        BidTransactionDTO transaction = new BidTransactionDTO(auction, UserSession.getInstance().getUser(),Double.parseDouble(bidAmount.getText()));
        ClientService.getInstance().sendMessage(new NetworkMessage("PLACE_BID", transaction));
    }

    public void cleanUp(){
        ClientService.getInstance().removeListener(this);
    }
}

package auction_client.controllers;

import auction_client.interfaces.AuctionUpdateListener;
import auction_client.Network.ClientService;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.AuctionDTO;
import auction_shared.dto.BidTransactionDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.List;
import java.time.LocalDateTime;

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
    Label timeLeft; // I still don't know what to do with this shit;

    @FXML
    TextField bidAmount;

    AuctionDTO auction = null;
    private Timeline countdownTimeline;

    public void initData(AuctionDTO auction) {
        this.auction = auction;
        updateData();
        ClientService.getInstance().addListener(this);
        startCountdown();
    }

    private void startCountdown() {
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdown()));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateCountdown() {
        long remaining = java.time.Duration.between(LocalDateTime.now(), auction.getEndTime()).getSeconds();
        if (remaining <= 0) {
            timeLeft.setText("HẾT GIỜ");
            countdownTimeline.stop();
        } else {
            timeLeft.setText(String.format("%02d:%02d", remaining / 60, remaining % 60));
        }
    }

    public void updateData() {
        Platform.runLater(() -> {
            currentPrice.setText(String.valueOf(auction.getCurrentHighestBid()));
            itemName.setText(auction.getItem().getName());
            buyOut.setText(String.valueOf(auction.getBuyOutPrice()));
            tickRate.setText(String.valueOf(auction.getTickSize()));
            description.setText(auction.getItem().getDescription());
        });
    }

    public void onUpdateReceived(NetworkMessage msg) {
        String action = msg.getAction();
        if (action.equals("UPDATE_BID")) {
            List<AuctionDTO> allRooms = (List<AuctionDTO>) msg.getData();
            Platform.runLater(() -> {
                boolean exist = false;
                for (AuctionDTO room : allRooms) {
                    if (room.getItem().getId().equals(auction.getItem().getId())) {
                        this.auction = room;
                        exist = true;
                        updateData();
                        break;
                    }
                }
                if (!exist) {
                    cleanUp();
                    switchToUserProductList();
                }
            });
        }
    }

    @FXML
    private void switchToUserProductList() {
        Stage stage = (Stage) itemName.getScene().getWindow();
        cleanUp();
        stage.close();
    }

    public void cleanUp() {
        ClientService.getInstance().removeListener(this);
    }
}
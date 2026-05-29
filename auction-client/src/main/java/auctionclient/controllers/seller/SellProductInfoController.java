package auctionclient.controllers.seller;

import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

import auctionclient.Network.ClientService;
import auctionclient.interfaces.AuctionUpdateListener;
import auctionclient.interfaces.Cleanable;
import auctionshared.Network.NetworkMessage;
import auctionshared.dto.AuctionDTO;
import auctionshared.dto.AuctionStatus;
import auctionshared.dto.BidTransactionDTO;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SellProductInfoController implements Initializable, AuctionUpdateListener, Cleanable {
    @FXML
    Label itemName;
    @FXML
    Label description;
    @FXML
    Label typeSpecificDisplay;
    @FXML
    Label currentPrice;
    @FXML
    Label buyOut;
    @FXML
    Label tickRate;
    @FXML
    Label timeLeft; // I still don't know what to do with this shit;
    @FXML
    LineChart<String, Number> bidHistory;
    @FXML
    NumberAxis yAxis;
    @FXML
    CategoryAxis xAxis;

    XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();

    AuctionDTO auction = null;
    private Timeline countdownTimeline;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        priceSeries.setName("Price");
        bidHistory.getData().add(priceSeries);
    }

    public void initData(AuctionDTO auction) {
        this.auction = auction;
        updateData();
        ClientService.getInstance().addListener(this);
        if (auction.getStatus().equals(AuctionStatus.ACTIVE)) {
            startCountdown();
        } else {
            timeLeft.setText(String.valueOf(auction.getStatus()));
        }
        ClientService.getInstance().sendMessage(new NetworkMessage("GET_BID_HISTORY", auction));
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
        DecimalFormat df = new DecimalFormat("#,###.##");
        Platform.runLater(() -> {
            currentPrice.setText("$" + df.format(auction.getCurrentHighestBid()));
            itemName.setText(auction.getItem().getName());
            buyOut.setText("$" + df.format(auction.getBuyOutPrice()));
            tickRate.setText("$" + df.format(auction.getTickSize()));
            description.setText(auction.getItem().getDescription());

            String attr = auction.getItem().getTypeSpecificAttribute();
            if (attr != null && !attr.isBlank()) {
                String label = auction.getItem().getTypeAttributeLabel() + ": " + attr;
                typeSpecificDisplay.setText(label);
            }
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
                        if (auction.getStatus().equals(AuctionStatus.ACTIVE)) {
                            ClientService.getInstance().sendMessage(new NetworkMessage("GET_BID_HISTORY", auction));
                        }
                        break;
                    }
                }
                if (auction.getStatus().equals(AuctionStatus.SOLD)) {
                    countdownTimeline.stop();
                    timeLeft.setText(String.valueOf(auction.getStatus()));
                }
                if (!exist) {
                    cleanup();
                    closeModal();
                }
            });
        } else if (action.equals("GET_BID_HISTORY")) {
            List<BidTransactionDTO> history = (List<BidTransactionDTO>) msg.getData();
            Platform.runLater(() -> {
                priceSeries.getData().clear();
                for (BidTransactionDTO transaction : history) {
                    priceSeries.getData().add(new XYChart.Data<>(transaction.getBidTime().toString(), transaction.getBidAmount()));
                }
            });
        }
    }

    @FXML
    private void closeModal() {
        cleanup();
        Stage stage = (Stage) itemName.getScene().getWindow();
        stage.close();
    }

    @Override
    public void cleanup() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        ClientService.getInstance().removeListener(this);
    }
}
package auction_client.controllers;

import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import auction_client.Network.ClientService;
import auction_client.UserSession;
import auction_client.interfaces.AuctionUpdateListener;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.*;
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
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

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
    TextField maxPrice; // autoBid price
    @FXML
    Button autoPlaceBid; // button for autoBid
    @FXML
    Label timeLeft; // I still don't know what to do with this shit; TT TT

    @FXML
    TextField bidAmount;
    @FXML
    Label typeSpecificDisplay;

    @FXML
    LineChart<String, Number> bidHistory;
    @FXML
    NumberAxis yAxis;
    @FXML
    CategoryAxis xAxis;

    // Tạo một Series dữ liệu (Đường bọc các điểm tọa độ)
    private XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();

    AuctionDTO auction = null;
    private Timeline countdownTimeline;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ClientService.getInstance().addListener(this);
        bidContainer.managedProperty().bind(bidContainer.visibleProperty());
        autoBidContainer.managedProperty().bind(autoBidContainer.visibleProperty());
        autoBidContainer.visibleProperty().bind(autoBidCheck.selectedProperty());
        bidContainer.visibleProperty().bind(autoBidCheck.selectedProperty().not());
        /// make the error disappear
        error.setOpacity(0.0);
        error.setManaged(false);
        error.setVisible(false);
        /// graph
        priceSeries.setName("Price");
        bidHistory.getData().add(priceSeries);
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
        if (action.equals("BID_SUCCESS")) {
            this.auction = (AuctionDTO) msg.getData();
            ClientService.getInstance().sendMessage(new NetworkMessage("GET_BID_HISTORY", auction));
            Platform.runLater(this::updateData);
        } else if (action.equals("BUYOUT_SUCCESS")) {
            this.auction = (AuctionDTO) msg.getData();
            Platform.runLater(() -> {
                bidAmount.setEditable(false);
                countdownTimeline.stop();
                timeLeft.setText(String.valueOf(auction.getStatus()));
            });
        } else if (action.equals("UPDATE_BID")) {
            List<AuctionDTO> auctions = (List<AuctionDTO>) msg.getData();
            Platform.runLater(() -> {
                boolean exist = false;
                for (AuctionDTO auction : auctions) {
                    if (auction.getAuctionId().equals(this.auction.getAuctionId())) {
                        this.auction = auction;
                        exist = true;
                        if (auction.getStatus().equals(AuctionStatus.ACTIVE)) {
                            ClientService.getInstance().sendMessage(new NetworkMessage("GET_BID_HISTORY", auction));
                        }
                        updateData();
                    }
                }
                if (!exist) {
                    cleanUp();
                    closeModal();
                }
            });
        } else if (action.equals("GET_BID_HISTORY")) {
            List<BidTransactionDTO> allTransactions = (List<BidTransactionDTO>) msg.getData();
            priceSeries.getData().clear();
            DateTimeFormatter formater = DateTimeFormatter.ofPattern("HH:mm");
            for (BidTransactionDTO transaction : allTransactions) {
                priceSeries.getData().add(new XYChart.Data<>(
                        transaction.getBidTime().format(formater),
                        transaction.getBidAmount()
                ));
            }

        } else if (action.equals("AUCTION_ENDED") || action.equals("AUCTION_SOLD")) {
            // Auction kết thúc hết giờ hoặc được mua ngay: chuyển modal sang read-only
            AuctionDTO incoming = (AuctionDTO) msg.getData();
            if (this.auction != null
                    && incoming.getAuctionId().equals(this.auction.getAuctionId())) {
                this.auction = incoming;
                Platform.runLater(() -> {
                    if (countdownTimeline != null) countdownTimeline.stop();
                    timeLeft.setText(String.valueOf(incoming.getStatus()));
                    bidAmount.setEditable(false);
                });
            }

        } else if (action.equals("REMOVE_ITEM")) {
            // Admin hủy auction: thông báo trong modal, khóa nhập liệu
            AuctionDTO removed = (AuctionDTO) msg.getData();
            if (this.auction != null
                    && removed.getAuctionId().equals(this.auction.getAuctionId())) {
                Platform.runLater(() -> {
                    if (countdownTimeline != null) countdownTimeline.stop();
                    timeLeft.setText("CANCELLED");
                    bidAmount.setEditable(false);
                    error.setVisible(true);
                    error.setManaged(true);
                    error.setOpacity(1.0);
                    error.setText("Sản phẩm này đã bị hủy bởi Admin.");
                    error.setTextFill(javafx.scene.paint.Color.ORANGE);
                });
            }
        }

    }


    public void initData(AuctionDTO auction) {
        this.auction = auction;
        updateData();
        if (auction.getStatus().equals(AuctionStatus.ACTIVE)) {
            startCountdown();
        }
        else {
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

    @FXML
    private void closeModal() {
        cleanUp();
        Stage stage = (Stage) tickRate.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void placeBidRequest() {
        /// missing the logic for auto-bidding.
        try {
            double amount = Double.parseDouble(bidAmount.getText());
            if (amount >= auction.getBuyOutPrice()) {
                BidTransactionDTO transaction = new BidTransactionDTO(
                        auction, UserSession.getInstance().getUser(),
                        auction.getBuyOutPrice(), LocalDateTime.now());
                ClientService.getInstance().sendMessage(new NetworkMessage("BUY_OUT", transaction));
                // change the label (notify) -> transparent
                error.setOpacity(0.0);
                error.setManaged(false);
                error.setVisible(false);
            } else if ((amount - auction.getCurrentHighestBid()) % auction.getTickSize() == 0) {
                BidTransactionDTO transaction = new BidTransactionDTO(
                        auction, UserSession.getInstance().getUser(),
                        amount, LocalDateTime.now());
                ClientService.getInstance().sendMessage(new NetworkMessage("PLACE_BID", transaction));
                // change the label (notify) -> transparent
                error.setOpacity(0.0);
                error.setManaged(false);
                error.setVisible(false);
            } else {
                // notify invalid bidAmount
                error.setVisible(true);
                error.setManaged(true);
                error.setOpacity(1.0);
                error.setText("Invalid Bid Amount!");
                error.setTextFill(Color.RED);
            }
        } catch (NumberFormatException e) {
            error.setVisible(true);
            error.setManaged(true);
            error.setOpacity(1.0);
            error.setText("Please enter a valid number!");
            error.setTextFill(Color.RED);
        }

    }

    public void cleanUp() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        ClientService.getInstance().removeListener(this);
    }
}

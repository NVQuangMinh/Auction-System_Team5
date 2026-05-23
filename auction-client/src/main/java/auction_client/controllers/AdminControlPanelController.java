package auction_client.controllers;

import auction_client.Network.ClientService;
import auction_client.interfaces.AuctionUpdateListener;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.AuctionDTO;
import auction_shared.dto.AuctionStatus;
import auction_shared.dto.ProductListResponse;
import auction_shared.dto.UserDTO;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;


import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminControlPanelController implements Initializable, AuctionUpdateListener {
    @FXML
    TableView<UserDTO> userTable;
    @FXML
    TableView<AuctionDTO> itemTable;

    @FXML
    TableColumn<UserDTO, String> colUserId;
    @FXML
    TableColumn<UserDTO, String> colUsername;
    @FXML
    TableColumn<UserDTO, String> colUserRole;
    @FXML
    TableColumn<UserDTO, Void> colUserAction;

    @FXML
    TableColumn<AuctionDTO, String> colItemName;
    @FXML
    TableColumn<AuctionDTO, Double> colItemPrice;
    @FXML
    TableColumn<AuctionDTO, AuctionStatus> colItemStatus;
    @FXML
    TableColumn<AuctionDTO, Void> colItemAction;

    private List<AuctionDTO> activeItems = new ArrayList<>();
    private List<AuctionDTO> endedItems = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUserTable();
        setupItemTable();
        ClientService.getInstance().addListener(this);
        ClientService.getInstance().sendMessage(new NetworkMessage("GET_USERS", null));
        ClientService.getInstance().sendMessage(new NetworkMessage("GET_PRODUCTS", null));
    }

    private void setupUserTable() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUserRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colUserAction.setCellFactory(param -> new TableCell<>() {
            private final Button actionButton = new Button("Ban");

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(actionButton);
                    UserDTO user = getTableView().getItems().get(getIndex());
                    actionButton.setOnAction(event -> handleBanUser(user));
                }
            }
        });
    }

    private void setupItemTable() {
        colItemName.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getItem().getName())
        );
        colItemPrice.setCellValueFactory(cellData ->
                new SimpleDoubleProperty(cellData.getValue().getCurrentHighestBid()).asObject()
        );
        colItemStatus.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getStatus())
        );

        colItemAction.setCellFactory(param -> new TableCell<>() {
            private final Button actionButton = new Button("Remove");

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(actionButton);
                    AuctionDTO auction = getTableView().getItems().get(getIndex());
                    actionButton.setOnAction(event -> handleRemoveAuction(auction));
                }
            }
        });
    }

    @Override
    public void onUpdateReceived(NetworkMessage msg) {
        String action = msg.getAction();
        if (action.equals("GET_USERS")) {
            List<UserDTO> users = (List<UserDTO>) msg.getData();
            userTable.getItems().clear();
            userTable.getItems().addAll(users);

        } else if (action.equals("GET_PRODUCTS")) {
            ProductListResponse response = (ProductListResponse) msg.getData();
            this.activeItems = response.getActiveAuctions() != null ? new ArrayList<>(response.getActiveAuctions()) : new ArrayList<>();
            this.endedItems = response.getEndedSaledAuctions() != null ? new ArrayList<>(response.getEndedSaledAuctions()) : new ArrayList<>();
            refreshItemTable();

        } else if (action.equals("UPDATE_BID")) {
            List<AuctionDTO> auctions = (List<AuctionDTO>) msg.getData();
            // UPDATE_BID chỉ chứa ACTIVE auctions từ RAM
            // Chỉ cập nhật phần ACTIVE, giữ nguyên ENDED/SOLD
            this.activeItems = auctions != null ? new ArrayList<>(auctions) : new ArrayList<>();
            refreshItemTable();
        }
    }

    private void refreshItemTable() {
        List<AuctionDTO> merged = new ArrayList<>(activeItems);
        merged.addAll(endedItems);
        itemTable.getItems().clear();
        itemTable.getItems().addAll(merged);
    }

    public void handleBanUser(UserDTO user) {
        ClientService.getInstance().sendMessage(new NetworkMessage("BAN_USER", user));
    }

    public void handleRemoveAuction(AuctionDTO auction) {
        ClientService.getInstance().sendMessage(new NetworkMessage("REMOVE_ITEM", auction));
    }
}

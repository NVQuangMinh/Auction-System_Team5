package auctionclient.controllers.admin;

import auctionclient.Network.ClientService;
import auctionclient.controllers.notification.UserPushUpNotificationController;
import auctionclient.interfaces.AuctionUpdateListener;
import auctionclient.interfaces.Cleanable;
import auctionshared.Network.NetworkMessage;
import auctionshared.dto.AuctionDTO;
import auctionshared.dto.AuctionStatus;

import auctionshared.dto.UserDTO;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminControlPanelController implements Initializable, AuctionUpdateListener, Cleanable {

    // ── User table ─────────────────────────────────────────────────────────────
    @FXML
    TableView<UserDTO> userTable;
    @FXML
    TableColumn<UserDTO, String> colUserId;
    @FXML
    TableColumn<UserDTO, String> colUsername;
    @FXML
    TableColumn<UserDTO, String> colUserRole;
    @FXML
    TableColumn<UserDTO, Void> colUserAction;

    // ── Item table ─────────────────────────────────────────────────────────────
    @FXML
    TableView<AuctionDTO> itemTable;
    @FXML
    TableColumn<AuctionDTO, String> colItemName;
    @FXML
    TableColumn<AuctionDTO, Double> colItemPrice;
    @FXML
    TableColumn<AuctionDTO, AuctionStatus> colItemStatus;
    @FXML
    TableColumn<AuctionDTO, Void> colItemAction;

    // ── Status filter ──────────────────────────────────────────────────────────
    @FXML
    RadioButton activeStatusRadio;
    @FXML
    RadioButton endedStatusRadio;
    @FXML
    ToggleGroup statusGroup;

    // ── State ──────────────────────────────────────────────────────────────────
    private List<AuctionDTO> activeItems = new ArrayList<>();
    private List<AuctionDTO> endedItems = new ArrayList<>();

    // ──────────────────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUserTable();
        setupItemTable();

        // Mặc định chọn tab Active
        activeStatusRadio.setSelected(true);

        ClientService.getInstance().addListener(this);
        ClientService.getInstance().sendMessage(new NetworkMessage("GET_USERS", null));
        ClientService.getInstance().sendMessage(new NetworkMessage("GET_ACTIVE_PRODUCTS", null));
    }

    @Override
    public void cleanup() {
        ClientService.getInstance().removeListener(this);
    }

    // ── Setup ──────────────────────────────────────────────────────────────────

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
        colItemName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItem().getName()));
        colItemPrice.setCellValueFactory(
                cellData -> new SimpleDoubleProperty(cellData.getValue().getCurrentHighestBid()).asObject());
        colItemStatus.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getStatus()));

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

    // ── Status filter ──────────────────────────────────────────────────────────

    @FXML
    private void onStatusSelected() {
        refreshCurrentView();
    }

    private void refreshCurrentView() {
        if (activeStatusRadio.isSelected()) {
            showActivePage();
        } else {
            requestEndedPage();
        }
    }

    // ── Active tab ─────────────────────────────────────────────────────────────

    private void showActivePage() {
        itemTable.getItems().setAll(activeItems);
    }

    // ── Ended tab & pagination ─────────────────────────────────────────────────

    private void requestEndedPage() {
        ClientService.getInstance().sendMessage(
                new NetworkMessage("GET_ENDED_PRODUCTS", null));
    }

    private void showEndedPage() {
        itemTable.getItems().setAll(endedItems);
    }

    // ── Network listener ───────────────────────────────────────────────────────

    @Override
    public void onUpdateReceived(NetworkMessage msg) {
        String action = msg.getAction();

        if (action.equals("GET_USERS")) {
            List<UserDTO> users = (List<UserDTO>) msg.getData();
            Platform.runLater(() -> {
                userTable.getItems().setAll(users);
            });

        } else if (action.equals("GET_ACTIVE_PRODUCTS")) {
            List<AuctionDTO> response = (List<AuctionDTO>) msg.getData();
            Platform.runLater(() -> {
                this.activeItems = response != null ? new ArrayList<>(response) : new ArrayList<>();
                refreshCurrentView();
            });

        } else if (action.equals("GET_ENDED_PRODUCTS")) {
            List<AuctionDTO> response = (List<AuctionDTO>) msg.getData();
            Platform.runLater(() -> {
                this.endedItems = response != null ? new ArrayList<>(response) : new ArrayList<>();
                if (endedStatusRadio.isSelected()) {
                    showEndedPage();
                }
            });

        } else if (action.equals("UPDATE_BID")) {
            List<AuctionDTO> auctions = (List<AuctionDTO>) msg.getData();
            Platform.runLater(() -> {
                this.activeItems = auctions != null ? new ArrayList<>(auctions) : new ArrayList<>();
                // Chỉ làm mới bảng nếu đang ở tab Active
                if (activeStatusRadio.isSelected()) {
                    showActivePage();
                }
            });

        } else if (action.equals("REMOVE_ITEM")) {
            // Auction ENDED/SOLD bị Admin ban: xóa khỏi danh sách local, refresh UI
            AuctionDTO removed = (AuctionDTO) msg.getData();
            Platform.runLater(() -> {
                endedItems.removeIf(a -> a.getItem().getId().equals(removed.getItem().getId()));
                if (endedStatusRadio.isSelected()) {
                    showEndedPage();
                }
            });

        } else if (action.equals("AUCTION_ENDED") || action.equals("AUCTION_SOLD")) {
            // Push-based: thông báo cho admin
            AuctionDTO dto = (AuctionDTO) msg.getData();
            String itemName = dto.getItem().getName();
            UserPushUpNotificationController.showNotification(
                    "Phiên đấu giá kết thúc: " + itemName, "INFO");
        }
    }

    // ── Actions ────────────────────────────────────────────────────────────────

    public void handleBanUser(UserDTO user) {
        ClientService.getInstance().sendMessage(new NetworkMessage("BAN_USER", user));
    }

    public void handleRemoveAuction(AuctionDTO auction) {
        ClientService.getInstance().sendMessage(new NetworkMessage("REMOVE_ITEM", auction));
    }
}

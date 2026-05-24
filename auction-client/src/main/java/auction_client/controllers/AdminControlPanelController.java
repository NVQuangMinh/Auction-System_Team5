package auction_client.controllers;

import auction_client.Network.ClientService;
import auction_client.interfaces.AuctionUpdateListener;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.AuctionDTO;
import auction_shared.dto.AuctionStatus;
import auction_shared.dto.EndedProductsRequest;
import auction_shared.dto.ProductListResponse;
import auction_shared.dto.UserDTO;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminControlPanelController implements Initializable, AuctionUpdateListener {

    private static final int PAGE_SIZE = 12;

    // ── User table ─────────────────────────────────────────────────────────────
    @FXML TableView<UserDTO> userTable;
    @FXML TableColumn<UserDTO, String> colUserId;
    @FXML TableColumn<UserDTO, String> colUsername;
    @FXML TableColumn<UserDTO, String> colUserRole;
    @FXML TableColumn<UserDTO, Void>   colUserAction;

    // ── Item table ─────────────────────────────────────────────────────────────
    @FXML TableView<AuctionDTO>          itemTable;
    @FXML TableColumn<AuctionDTO, String>        colItemName;
    @FXML TableColumn<AuctionDTO, Double>        colItemPrice;
    @FXML TableColumn<AuctionDTO, AuctionStatus> colItemStatus;
    @FXML TableColumn<AuctionDTO, Void>          colItemAction;

    // ── Status filter ──────────────────────────────────────────────────────────
    @FXML RadioButton activeStatusRadio;
    @FXML RadioButton endedStatusRadio;
    @FXML ToggleGroup statusGroup;

    // ── Pagination ─────────────────────────────────────────────────────────────
    @FXML HBox  paginationBox;
    @FXML Label prevButton;
    @FXML Label pageInfoLabel;
    @FXML Label nextButton;

    // ── State ──────────────────────────────────────────────────────────────────
    private List<AuctionDTO> activeItems = new ArrayList<>();
    private List<AuctionDTO> endedItems  = new ArrayList<>();
    private int endedPage       = 0;
    private int endedTotalCount = 0;

    // ──────────────────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUserTable();
        setupItemTable();

        // Mặc định chọn tab Active, ẩn pagination
        activeStatusRadio.setSelected(true);
        paginationBox.setVisible(false);
        paginationBox.setManaged(false);

        prevButton.setOnMouseClicked(e -> handlePrevPage());
        nextButton.setOnMouseClicked(e -> handleNextPage());

        ClientService.getInstance().addListener(this);
        ClientService.getInstance().sendMessage(new NetworkMessage("GET_USERS", null));
        ClientService.getInstance().sendMessage(new NetworkMessage("GET_PRODUCTS", null));
    }

    /** Gọi khi rời màn hình Admin để tránh memory leak / zombie listener. */
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
        colItemName.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getItem().getName()));
        colItemPrice.setCellValueFactory(cellData ->
                new SimpleDoubleProperty(cellData.getValue().getCurrentHighestBid()).asObject());
        colItemStatus.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getStatus()));

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
        endedPage = 0;
        refreshCurrentView();
    }

    private void refreshCurrentView() {
        if (activeStatusRadio.isSelected()) {
            showActivePage();
        } else {
            requestEndedPage(endedPage);
        }
    }

    // ── Active tab ─────────────────────────────────────────────────────────────

    private void showActivePage() {
        itemTable.getItems().setAll(activeItems);
        paginationBox.setVisible(false);
        paginationBox.setManaged(false);
        pageInfoLabel.setText("1 / 1");
    }

    // ── Ended tab & pagination ─────────────────────────────────────────────────

    private void requestEndedPage(int page) {
        ClientService.getInstance().sendMessage(
                new NetworkMessage("GET_ENDED_PRODUCTS",
                        new EndedProductsRequest(null, page, PAGE_SIZE)));
    }

    private void handlePrevPage() {
        if (endedStatusRadio.isSelected() && endedPage > 0) {
            endedPage--;
            requestEndedPage(endedPage);
        }
    }

    private void handleNextPage() {
        if (!endedStatusRadio.isSelected()) return;
        int totalPages = getTotalEndedPages();
        if (endedPage + 1 < totalPages) {
            endedPage++;
            requestEndedPage(endedPage);
        }
    }

    private int getTotalEndedPages() {
        if (endedTotalCount == 0) return 1;
        return (int) Math.ceil((double) endedTotalCount / PAGE_SIZE);
    }

    private void showEndedPage() {
        itemTable.getItems().setAll(endedItems);

        paginationBox.setVisible(true);
        paginationBox.setManaged(true);

        int totalPages = getTotalEndedPages();
        updatePageInfo(endedPage + 1, totalPages);
        updateNavButtons();
    }

    private void updatePageInfo(int current, int total) {
        pageInfoLabel.setText(current + " / " + total);
    }

    private void updateNavButtons() {
        int totalPages = getTotalEndedPages();
        boolean atFirst = endedPage == 0;
        boolean atLast  = endedPage >= totalPages - 1;

        String enabledStyle  = "-fx-text-fill: #138eff; -fx-opacity: 1.0; -fx-cursor: hand;";
        String disabledStyle = "-fx-text-fill: #aaa;    -fx-opacity: 0.4;";

        prevButton.setDisable(atFirst);
        nextButton.setDisable(atLast);
        prevButton.setStyle(atFirst ? disabledStyle : enabledStyle);
        nextButton.setStyle(atLast  ? disabledStyle : enabledStyle);
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

        } else if (action.equals("GET_PRODUCTS")) {
            ProductListResponse response = (ProductListResponse) msg.getData();
            Platform.runLater(() -> {
                this.activeItems      = response.getActiveAuctions()      != null ? new ArrayList<>(response.getActiveAuctions())      : new ArrayList<>();
                this.endedItems       = response.getEndedSaledAuctions()  != null ? new ArrayList<>(response.getEndedSaledAuctions())  : new ArrayList<>();
                this.endedTotalCount  = response.getEndedTotalCount();
                refreshCurrentView();
            });

        } else if (action.equals("GET_ENDED_PRODUCTS")) {
            ProductListResponse response = (ProductListResponse) msg.getData();
            Platform.runLater(() -> {
                this.endedItems      = response.getEndedSaledAuctions()  != null ? new ArrayList<>(response.getEndedSaledAuctions())  : new ArrayList<>();
                this.endedTotalCount = response.getEndedTotalCount();
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

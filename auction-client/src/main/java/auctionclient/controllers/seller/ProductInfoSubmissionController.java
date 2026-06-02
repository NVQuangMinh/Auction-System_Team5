package auctionclient.controllers.seller;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.UUID;

import auctionclient.Network.ClientService;
import auctionclient.UserSession;
import auctionclient.exception.InvalidPriceException;
import auctionclient.exception.InvalidTickSizeException;
import auctionshared.Network.NetworkMessage;
import auctionshared.dto.AuctionDTO;
import auctionshared.dto.AuctionStatus;
import auctionshared.dto.ItemDTO;
import auctionshared.dto.ItemType;
import auctionshared.dto.UserDTO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class ProductInfoSubmissionController implements Initializable {
    @FXML
    private TextField productName;
    @FXML
    private TextArea productDescription;
    @FXML
    private TextField startingPrice;
    @FXML
    private TextField buyoutPrice;
    @FXML
    private TextField tickSize;
    @FXML
    private CheckBox antiSnippingCheckbox;
    @FXML
    private ImageView addImageButton;
    @FXML
    private Button submitButton;
    @FXML
    private TextField bidDurationField;
    @FXML
    private TextField typeSpecificField;
    @FXML
    Label error;

    /// item below
    private String name;
    private String description;
    private double startPriceVal;
    private double buyOutPriceVal;
    private double tickSizeVal;
    private Integer bidDuration;
    private LocalDateTime startBidDate;
    private LocalDateTime endBidDate;
    private ItemType type;
    private boolean antiSnipping;

    @FXML
    private ChoiceBox<String> types;
    private final String[] options = { "Art", "Electronic", "Vehicle" };

    public void initialize(URL url, ResourceBundle resourceBundle) {
        error.setVisible(false);
        error.setManaged(false);
        error.setOpacity(0.0);
        types.getItems().addAll(options);
    }


    private void handleSubmit() {
        name = productName.getText().trim();
        description = productDescription.getText().trim();
        String startPriceStr = startingPrice.getText().trim();
        String buyOutPriceStr = buyoutPrice.getText().trim();
        String tickStr = tickSize.getText().trim();
        String bidDurStr = bidDurationField.getText().trim();

        if (name.isBlank() || description.isBlank() ||
                startPriceStr.isBlank() || buyOutPriceStr.isBlank() ||
                tickStr.isBlank() || bidDurStr.isBlank()) {
            throw new IllegalArgumentException("Xin hãy điền đầy đủ thông tin sản phẩm");
        }

        String selectedType = types.getValue();
        if (selectedType == null) {
            throw new IllegalArgumentException("Xin hãy chọn loại sản phẩm");
        }

        type = switch (selectedType) {
            case "Art" -> ItemType.ARTS;
            case "Electronic" -> ItemType.ELECTRONICS;
            case "Vehicle" -> ItemType.VEHICLES;
            default -> null;
        };
        if (type == null) {
            throw new IllegalArgumentException("Xin hãy chọn loại sản phẩm");
        }


        // ktra số
        try {
            startPriceVal = Double.parseDouble(startPriceStr);
            buyOutPriceVal = Double.parseDouble(buyOutPriceStr);
            tickSizeVal = Double.parseDouble(tickStr);
        } catch (NumberFormatException e) {
            throw new InvalidPriceException("Xin hãy điền các số phù hợp");
        }

        if (buyOutPriceVal <= startPriceVal) {
            throw new InvalidPriceException("Giá mua của sản phẩm không được nhỏ hơn hoặc bằng giá khởi điểm");
        }

        if ((buyOutPriceVal - startPriceVal) % tickSizeVal != 0 || tickSizeVal <= 0) {
            throw new InvalidTickSizeException("Xin hãy điền bước giá hợp lệ");
        }

        try {
            bidDuration = Integer.parseInt(bidDurStr);
            if (bidDuration <= 0) {
                throw new IllegalArgumentException("Xin hãy điền thời gian hợp lệ");
            }
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Xin hãy điền số nguyên");
        }

        antiSnipping = antiSnippingCheckbox.isSelected();

        startBidDate = LocalDateTime.now();
        endBidDate = startBidDate.plusMinutes(bidDuration);
    }

    @FXML
    public void addItem(ActionEvent event) {
        try {
            handleSubmit();
            error.setVisible(false);
            error.setManaged(false);
            error.setOpacity(0.0);

            UserDTO owner = UserSession.getInstance().getUser();
            String newId = UUID.randomUUID().toString();
            String specificAttr = typeSpecificField.getText().trim();
            ItemDTO item = new ItemDTO(newId, name, description, owner, type, specificAttr);
            AuctionDTO auction = new AuctionDTO(
                    item,
                    AuctionStatus.ACTIVE,
                    startPriceVal,
                    buyOutPriceVal,
                    tickSizeVal,
                    startBidDate,
                    endBidDate,
                    antiSnipping,
                    null,
                    startPriceVal
            );

            ClientService.getInstance().sendMessage(new NetworkMessage("SELL", auction));
            switchToUserProductList(event);
        } catch (IllegalArgumentException | InvalidPriceException | InvalidTickSizeException e) {
            error.setVisible(true);
            error.setOpacity(1.0);
            error.setManaged(true);
            error.setText(e.getMessage());
            error.setTextFill(Color.RED);
        } catch (Exception e) {
            error.setVisible(true);
            error.setOpacity(1.0);
            error.setManaged(true);
            error.setText("Lỗi không xác định...");
            error.setTextFill(Color.RED);
        }
    }

    @FXML
    private void switchToUserProductList(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

}

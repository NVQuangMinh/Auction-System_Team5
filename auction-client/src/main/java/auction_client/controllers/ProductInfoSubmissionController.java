package auction_client.controllers;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.UUID;

import auction_client.Network.ClientService;
import auction_client.UserSession;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.AuctionDTO;
import auction_shared.dto.AuctionStatus;
import auction_shared.dto.ItemDTO;
import auction_shared.dto.ItemType;
import auction_shared.dto.UserDTO;
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

    @FXML
    private void handleAddImage() {
        System.out.println("Add image clicked");
    }

    private boolean handleSubmit() {
        name = productName.getText().trim();
        description = productDescription.getText().trim();
        String startPriceStr = startingPrice.getText().trim();
        String buyOutPriceStr = buyoutPrice.getText().trim();
        String tickStr = tickSize.getText().trim();
        String bidDurStr = bidDurationField.getText().trim();

        if (name.isBlank() || description.isBlank() ||
                startPriceStr.isBlank() || buyOutPriceStr.isBlank() ||
                tickStr.isBlank() || bidDurStr.isBlank()) {
            return false;
        }

        String selectedType = types.getValue();
        if (selectedType == null)
            return false;
        type = switch (selectedType) {
            case "Art" -> ItemType.ARTS;
            case "Electronic" -> ItemType.ELECTRONICS;
            case "Vehicle" -> ItemType.VEHICLES;
            default -> null;
        };
        if (type == null)
            return false;

        // ktra số
        try {
            startPriceVal = Double.parseDouble(startPriceStr);
            buyOutPriceVal = Double.parseDouble(buyOutPriceStr);
            tickSizeVal = Double.parseDouble(tickStr);
        } catch (NumberFormatException e) {
            return false;
        }

        if(buyOutPriceVal <= startPriceVal || tickSizeVal <= 0)
            return false;

        if((buyOutPriceVal - startPriceVal) % tickSizeVal != 0)
            return false;

        try {
            bidDuration = Integer.parseInt(bidDurStr);
            if (bidDuration <= 0)
                return false;
        } catch (NumberFormatException e) {
            return false;
        }

        antiSnipping = antiSnippingCheckbox.isSelected();

        startBidDate = LocalDateTime.now();
        endBidDate = startBidDate.plusMinutes(bidDuration);
        return true;
    }

    @FXML
    public void addItem(ActionEvent event) {
        if (handleSubmit()) {
            error.setVisible(false);
            error.setManaged(false);
            error.setOpacity(0.0);

            UserDTO owner = UserSession.getInstance().getUser();
            String newId = UUID.randomUUID().toString();
            ItemDTO item = new ItemDTO(newId, name, description, owner, type);
            AuctionDTO auction = new AuctionDTO(
                    item,
                    type,
                    AuctionStatus.ACTIVE,
                    startPriceVal,
                    buyOutPriceVal,
                    tickSizeVal,
                    startBidDate,
                    endBidDate,
                    antiSnipping,
                    null,
                    startPriceVal);

            ClientService.getInstance().sendMessage(new NetworkMessage("SELL", auction));
            switchToUserProductList(event);
        } else {
            error.setVisible(true);
            error.setOpacity(1.0);
            error.setManaged(true);
            error.setText("Please fill in all fields correctly!");
            error.setTextFill(Color.RED);
        }
    }

    @FXML
    private void switchToUserProductList(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

}

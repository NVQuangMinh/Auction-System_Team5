package auction_client.controllers;

import auction_client.Network.ClientService;
import auction_client.UserSession;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.ItemDTO;
import auction_shared.dto.ItemType;
import auction_shared.dto.UserDTO;
import auction_shared.dto.AuctionDTO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.UUID;


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
    public Button submitButton;
    @FXML
    Label error;

    ///  item below
    private String name;
    private String description;
    private String startPrice;
    private String buyOutPrice;
    private String tick;
    private boolean antiSnipping;
    private ItemType type;
    private LocalDateTime startBidDate;




    @FXML
    private ChoiceBox<String> types;
    private final String[] options = {"Art","Electronic","Vehicle"};
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

    @FXML
    private boolean handleSubmit() {
        name = productName.getText();
        description = productDescription.getText();
        startPrice = startingPrice.getText();
        buyOutPrice = buyoutPrice.getText();
        tick = tickSize.getText();
        antiSnipping = antiSnippingCheckbox.isSelected();
        if (types.getValue() == null){
            type = null;
        }
        else if (types.getValue().equals("Art")){
            type = ItemType.ARTS;
        }
        else if (types.getValue().equals("Electronic")){
            type = ItemType.ELECTRONICS;
        }
        else if (types.getValue().equals("Vehicle")){
            type = ItemType.VEHICLES;
        }
        else {
            type = null;
        }
        startBidDate = LocalDateTime.now();
        // end time == null
        return !name.isBlank() &&
                !description.isBlank() &&
                !startPrice.isBlank() &&
                !buyOutPrice.isBlank() &&
                !tick.isBlank() &&
                type != null;
        // missing end time
    }

    @FXML
    public void addItem(ActionEvent event){
        if (handleSubmit()){
            error.setVisible(false);
            error.setManaged(false);
            error.setOpacity(0.0);

            UserDTO owner = UserSession.getInstance().getUser();
            String newId = UUID.randomUUID().toString();
            ItemDTO item = new ItemDTO(newId,name,description,owner,type);
            AuctionDTO auction = new AuctionDTO(
                    item,
                    Double.parseDouble(startPrice),
                    Double.parseDouble(buyOutPrice),
                    Double.parseDouble(tick),
                    startBidDate,
                    startBidDate,
                    Double.parseDouble(startPrice)
            );

            ClientService.getInstance().sendMessage(new NetworkMessage("SELL",auction));
            switchToUserProductList(event);
        }
        else {
            error.setManaged(true);
            error.setVisible(true);
            error.setOpacity(1.0);
            error.setText("Require to fill every plank!");
            error.setTextFill(Color.RED);
        }

    }

    @FXML
    private void switchToUserProductList(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

}

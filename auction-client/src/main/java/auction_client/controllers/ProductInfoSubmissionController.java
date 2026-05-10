package auction_client.controllers;

import auction_client.Network.ClientService;
import auction_shared.Network.NetworkMessage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;


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

    ///  item below
    private String name;
    private String description;
    private String startPrice;
    private String buyPrice;
    private String tick;
    private boolean antiSnipping;
    private LocalDateTime startBidDate;
    private LocalDateTime endBidDate;



    @FXML
    private ChoiceBox<String> types;
    private final String[] options = {"Art","Electric","Vehicle"};
    public void initialize(URL url, ResourceBundle resourceBundle) {
        types.getItems().addAll(options);
    }

    @FXML
    private void handleAddImage() {
        System.out.println("Add image clicked");
    }

    @FXML
    private void handleSubmit() {
        name = productName.getText();
        description = productDescription.getText();
        startPrice = startingPrice.getText();
        buyPrice = buyoutPrice.getText();
        tick = tickSize.getText();
        antiSnipping = antiSnippingCheckbox.isSelected();
        startBidDate = LocalDateTime.now();
        // missing start and end time
    }

    @FXML
    public void addItem(ActionEvent event){
        handleSubmit();

        // owner = UserSession.getUser();
        // create an Item object (need an owner)
        // create an auctionDTO object (need an item)
        ClientService.getInstance().sendMessage(new NetworkMessage("SELL",null)); // null = auctionDTO
        switchToUserProductList(event);
    }

    @FXML
    private void switchToUserProductList(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

}

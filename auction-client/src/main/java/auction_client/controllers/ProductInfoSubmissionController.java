package auction_client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.net.URL;
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

    @FXML
    private ChoiceBox<String> types;
    private String[] options = {"Art","Electric","Vehicle"};
    public void initialize(URL url, ResourceBundle resourceBundle) {
        types.getItems().addAll(options);
    }

    @FXML
    private void handleAddImage() {
        System.out.println("Add image clicked");
    }

    @FXML
    private void handleSubmit() {
        String name = productName.getText();
        String description = productDescription.getText();
        String startPrice = startingPrice.getText();
        String buyPrice = buyoutPrice.getText();
        String tick = tickSize.getText();
        boolean antiSnipping = antiSnippingCheckbox.isSelected();

        System.out.println("Product Submitted: " + name);
    }

    @FXML
    public void addItem(ActionEvent event){
        // owner = UserSession.getUser();
        // create an Item object
        // create an auction object
        // new NetworkMessage("SELL",auction);
        //close the window after finish adding item
        switchToUserProductList(event);
    }

    @FXML
    private void switchToUserProductList(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

}

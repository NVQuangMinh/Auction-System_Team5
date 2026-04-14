package auction_client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;


public class ProductInfoSubmissionController {
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
    private void switchToUserProductList(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

}

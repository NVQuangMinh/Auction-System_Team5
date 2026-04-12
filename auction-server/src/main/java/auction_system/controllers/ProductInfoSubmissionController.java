package auction_system.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

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
        // Logic for adding an image
        System.out.println("Add image clicked");
    }

    @FXML
    private void handleSubmit() {
        // Logic for submitting product info
        String name = productName.getText();
        String description = productDescription.getText();
        String startPrice = startingPrice.getText();
        String buyPrice = buyoutPrice.getText();
        String tick = tickSize.getText();
        boolean antiSnipping = antiSnippingCheckbox.isSelected();

        System.out.println("Product Submitted: " + name);
    }

}

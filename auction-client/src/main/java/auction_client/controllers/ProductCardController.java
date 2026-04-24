package auction_client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;



public class ProductCardController {
    @FXML
    private Label itemNameLabel;
    @FXML
    private Label stateLabel;
    @FXML
    private StackPane productImageStackPane;
    @FXML
    private ImageView productImageView;
    @FXML
    private Label currentPriceLabel;
    @FXML
    private Button buyOutButton;
    @FXML
    private Label descriptionLabel;
    @FXML
    public void initialize() {
        Rectangle clip = new Rectangle(
            productImageView.getFitWidth(), 
            productImageView.getFitHeight()
        );
        clip.setArcWidth(40);
        clip.setArcHeight(40);
        productImageView.setClip(clip);
    }

    public void setData(String name, String state, String price, String description, Image image) {
        itemNameLabel.setText(name);
        stateLabel.setText(state);
        currentPriceLabel.setText(price);
        descriptionLabel.setText(description);
        if (image != null) {
            productImageView.setImage(image);
        }
    }
}

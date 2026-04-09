package auction_system.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import java.io.IOException;

public class WebMenuBarController {
    @FXML
    public Label welcome;
    @FXML
    public ImageView logoutImage;
    @FXML
    public Button productsButton;

    Scene scene;
    Stage stage;
    Parent root;
    public void setWelcomeUsername(String username) {
        if (username != null && !username.equals("")) {
            welcome.setText("Welcome, " + "\n" + username);
        }
    }
    @FXML
    public void switchToProductScene(ActionEvent event) throws IOException{
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/auction_system/ProductScene.fxml"));
        root = fxmlLoader.load();
        stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
        stage.centerOnScreen();
        stage.show();
    }

    @FXML
    public void logOut(MouseEvent event) throws IOException {
        Alert logOutAlert = new Alert(Alert.AlertType.CONFIRMATION);
        logOutAlert.setTitle("Logout");
        logOutAlert.setHeaderText("REDIRECT TO THE SIGN-IN PAGE...");
        logOutAlert.setContentText("ARE YOU SURE YOU WANNA LOGOUT?");
        if (logOutAlert.showAndWait().get() == ButtonType.OK){
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/auction_system/SignInScene.fxml"));
            root = fxmlLoader.load();
            stage = (Stage)((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.centerOnScreen();
            stage.show();
        }
    }
}

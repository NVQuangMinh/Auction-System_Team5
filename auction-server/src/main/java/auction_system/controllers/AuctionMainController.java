package auction_system.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class AuctionMainController {
    @FXML
    public Label welcome;
    @FXML
    public ImageView logoutImage;
    public Scene Scene;

    Stage stage;
    Parent root;
    public void WelcomUsername(String username){
        welcome.setText("Welcome, " + "\n" + username);
    }

    public void logOut(MouseEvent event) throws IOException {
        Alert logOutAlert = new Alert(Alert.AlertType.CONFIRMATION);
        logOutAlert.setTitle("Logout");
        logOutAlert.setHeaderText("REDIRECT TO THE SIGN-IN PAGE...");
        logOutAlert.setContentText("ARE YOU SURE YOU WANNA LOGOUT?");
        if (logOutAlert.showAndWait().get() == ButtonType.OK){
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/auction_system/SignInScene.fxml"));
            root = fxmlLoader.load();
            stage = (Stage)((Node) event.getSource()).getScene().getWindow();
            Scene = new Scene(root);
            stage.setScene(Scene);
            stage.show();
        }
    }
}

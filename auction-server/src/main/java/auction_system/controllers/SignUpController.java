package auction_system.controllers;

import auction_system.entity.Bidder;
import auction_system.entity.Session;
import auction_system.entity.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class SignUpController {
    @FXML
    public PasswordField password;
    @FXML
    public PasswordField confirmpassword;
    @FXML
    public TextField username;
    Stage stage;
    Scene scene;
    Parent root;

    @FXML
    public void switchToMainScene(ActionEvent event) throws IOException {
        String newUsername = username.getText();
        String newPassword = password.getText();

        if (!newUsername.isEmpty() && !newPassword.isEmpty()) {
            User newUser = new Bidder(newUsername, newPassword);
            Session.getInstance().setCurrentUser(newUser);

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/auction_system/AuctionMain.fxml"));
            root = fxmlLoader.load();

            try {
                AuctionMainController mainController = fxmlLoader.getController();
                mainController.WelcomUsername(newUser.getUsername());
            } catch (Exception e) {
            }

            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.centerOnScreen();
            stage.setMaximized(true);
            stage.show();
        }
    }

    public void switchToSignUpScene(ActionEvent event) throws IOException {
        System.out.println("sign up");
    }

    @FXML
    public void switchToSignInScene(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/auction_system/SignInScene.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
        stage.centerOnScreen();
        stage.show();
    }
}
package auction_client.controllers;

import auction_system.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class SignInController {
    @FXML
    public TextField username;
    @FXML
    public PasswordField password;
    
    Stage stage;
    Parent root;

    @FXML
    public void switchToMainScene(ActionEvent event) throws IOException {
        String userText = username.getText().trim();
        if (!userText.isEmpty()) {
            UserSession.getInstance().setUsername(userText);
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/auction_system/AuctionMain.fxml"));
            root = fxmlLoader.load();

            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.centerOnScreen();
            stage.setMaximized(true);
            stage.show();
        }
    }

    @FXML
    public void switchToSignInScene(ActionEvent event) throws IOException {
        System.out.println("sign in");
    }

    @FXML
    public void switchToSignUpScene(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/auction_system/SignUpScene.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
        stage.centerOnScreen();
        stage.show();
    }
}

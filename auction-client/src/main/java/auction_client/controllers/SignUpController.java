package auction_client.controllers;

import auction_system.UserSession;
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
    Parent root;

    @FXML
    public void switchToMainScene(ActionEvent event) throws IOException{
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/auction_system/AuctionMain.fxml"));
        String usernameText = username.getText();
        if (!usernameText.equals("") || !usernameText.isBlank()){
            UserSession.getInstance().setUsername(usernameText);
            root = fxmlLoader.load();
            stage = (Stage)((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.centerOnScreen();
            stage.setMaximized(true);
            stage.show();
        }

    }
    public void switchToSignUpScene(ActionEvent event) throws IOException{
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

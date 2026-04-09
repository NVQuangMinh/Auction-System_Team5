package auction_system.controllers;

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
    public void switchToMainScene(ActionEvent event) throws IOException{
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/auction_system/AuctionMain.fxml"));
        root = fxmlLoader.load();
        
        AuctionMainController auctionMainController = fxmlLoader.getController();
        WebMenuBarController webMenuBarController = auctionMainController.getWebMenuBarController();
        String temp = username.getText();
        if (!temp.equals("")){
            webMenuBarController.setWelcomeUsername(temp);
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

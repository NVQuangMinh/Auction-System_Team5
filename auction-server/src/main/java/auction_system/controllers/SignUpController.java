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
    public void switchToMainScene(ActionEvent event) throws IOException{
        root = FXMLLoader.load(getClass().getResource("/auction_system/AuctionMain.fxml"));
        stage = (Stage)((Node) event.getSource()).getScene().getWindow();
//        scene = new Scene(root);
//        stage.setScene(scene);
        stage.getScene().setRoot(root);
        stage.centerOnScreen();
        stage.setMaximized(true);
        stage.show();
    }
    public void switchToSignUpScene(ActionEvent event) throws IOException{
        System.out.println("sign up");
    }
    public void switchToSignInScene(ActionEvent event) throws IOException{
        root = FXMLLoader.load(getClass().getResource("/auction_system/SignInScene.fxml"));
        stage = (Stage)((Node) event.getSource()).getScene().getWindow();
//        scene = new Scene(root);
//        stage.setScene(scene);
        stage.getScene().setRoot(root);
        stage.centerOnScreen();
        stage.show();
    }
}

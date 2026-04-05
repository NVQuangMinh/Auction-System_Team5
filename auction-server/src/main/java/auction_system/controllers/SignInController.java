package auction_system.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
    Scene scene;
    Parent root;
    public void switchToMainScene(ActionEvent event) throws IOException{
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/auction_system/AuctionMain.fxml"));
        root = fxmlLoader.load();
        AuctionMainController mainController = fxmlLoader.getController();
        String temp = username.getText();
        if (!temp.equals("")){
            mainController.WelcomUsername(temp);
            stage = (Stage)((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.centerOnScreen();
            stage.setMaximized(true);
            stage.show();
        }
    }
    public void switchToSignInScene(ActionEvent event) throws IOException {
        System.out.println("sign in");
    }
    public void switchToSignUpScene(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/auction_system/SignUpScene.fxml"));
        stage = (Stage)((Node) event.getSource()).getScene().getWindow();
//        scene = new Scene(root);
//        stage.setScene(scene);
        stage.getScene().setRoot(root);
        stage.centerOnScreen();
        stage.show();
    }
}

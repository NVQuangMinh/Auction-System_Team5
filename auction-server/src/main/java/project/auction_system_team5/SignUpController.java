package project.auction_system_team5;

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
    public PasswordField password;
    public PasswordField confirmpassword;
    public TextField username;
    Stage stage;
    Scene scene;
    Parent root;
    public void switchToMainScene(ActionEvent event){
        System.out.println("Main");
    }
    public void switchToSignUpScene(ActionEvent event) throws IOException{
        System.out.println("sign up");
    }
    public void switchToSignInScene(ActionEvent event) throws IOException{
        root = FXMLLoader.load(getClass().getResource("SignInScene.fxml"));
        stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}

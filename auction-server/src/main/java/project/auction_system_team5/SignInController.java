package project.auction_system_team5;

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
    public void switchToMainScene(ActionEvent event){
    }
    public void switchToSignInScene(ActionEvent event) throws IOException {
        System.out.println("sign in");
    }
    public void switchToSignUpScene(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("SignUpScene.fxml"));
        stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}

package auction_client.controllers;

import auction_client.UserSession;
import auction_client.entity.Bidder;
import auction_client.entity.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
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
    public void switchToMainScene(ActionEvent event) throws IOException {
        String inputUsername = username.getText().trim();
        String inputPassword = password.getText();
        String inputPasswordConfirm = confirmpassword.getText();

        if (!inputUsername.isEmpty() && !inputPassword.isEmpty() && !inputPasswordConfirm.isEmpty()) {
            if (!inputPassword.equals(inputPasswordConfirm)) {
                showAlert(Alert.AlertType.ERROR, "Lỗi Mật Khẩu", "Mật khẩu xác nhận không khớp. Vui lòng nhập lại!");
                confirmpassword.clear();
                return;
            }
            User newUser = new Bidder(inputUsername, inputPassword);

            UserSession.getInstance().setUsername(inputUsername);

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/auction_client/AuctionMain.fxml"));
            root = fxmlLoader.load();

            stage = (Stage)((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.centerOnScreen();
            stage.setMaximized(true);
            stage.show();
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void switchToSignUpScene(ActionEvent event) throws IOException {
        System.out.println("sign up");
    }

    @FXML
    public void switchToSignInScene(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/auction_client/SignInScene.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
        stage.centerOnScreen();
        stage.show();
    }
}
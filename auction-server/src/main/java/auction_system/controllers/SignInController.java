package auction_system.controllers;

import auction_system.entity.Bidder;
import auction_system.entity.Session;
import auction_system.entity.User;
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
        String inputUsername = username.getText();
        String inputPassword = password.getText();

        if (!inputUsername.isEmpty() && !inputPassword.isEmpty()) {
            User loggedInUser = authenticate(inputUsername, inputPassword);

            if (loggedInUser != null) {
                Session.getInstance().setCurrentUser(loggedInUser);

                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/auction_system/AuctionMain.fxml"));
                root = fxmlLoader.load();

                try {
                    AuctionMainController mainController = fxmlLoader.getController();
                    mainController.WelcomUsername(loggedInUser.getUsername());
                } catch (Exception e) {
                }

                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.getScene().setRoot(root);
                stage.centerOnScreen();
                stage.setMaximized(true);
                stage.show();
            } else {
                System.out.println("Tài khoản không tồn tại. Đang chuyển hướng sang Đăng ký...");
                switchToSignUpScene(event);
            }
        }
    }

    private User authenticate(String username, String password) {

        // TODO: Thêm lệnh database vào

        if (username.equals("admin") && password.equals("123")) {
            return new Bidder(username, password);
        }

        return null;
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
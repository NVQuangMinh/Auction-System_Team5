package auction_client.controllers;

import auction_client.DatabaseConnector;
import auction_client.UserSession;
import auction_client.entity.Bidder;
import auction_client.entity.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
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
        String inputUsername = username.getText().trim();
        String inputPassword = password.getText();

        if (!inputUsername.isEmpty() && !inputPassword.isEmpty()) {
            try {
                //TODO: sửa lại lấy từ database
                User authenticatedUser = authenticate(inputUsername, inputPassword);

                if (authenticatedUser != null) {
                    UserSession.getInstance().setUsername(inputUsername);

                    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/auction_system/AuctionMain.fxml"));
                    root = fxmlLoader.load();

                    stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.getScene().setRoot(root);
                    stage.centerOnScreen();
                    stage.setMaximized(true);
                    stage.show();
                } else {
                    System.out.println("Account not existing");
                    switchToSignUpScene(event);
                }
            }
            catch (IllegalArgumentException e) {
                showAlert("Wrong password", e.getMessage(), Alert.AlertType.ERROR);
                password.clear();
            }
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }


    private User authenticate(String username, String password) {
        // TODO: Thay bằng lệnh SQL thực tế sau này (Ví dụ: return userDao.login(username, password);)
        if (DatabaseConnector.checkLogin(username, password)) {
            // Nếu đăng nhập thành công, trả về đối tượng User
            return new Bidder(username, password);
        }
        return null;

        // TODO: Mô phỏng tài khoản admin

    }

    @FXML
    public void switchToSignInScene(ActionEvent event) throws IOException {
        System.out.println("sign in");
    }

    @FXML
    public void switchToSignUpScene(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/auction_client/SignUpScene.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
        stage.centerOnScreen();
        stage.show();
    }
}
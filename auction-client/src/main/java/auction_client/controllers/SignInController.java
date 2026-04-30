package auction_client.controllers;

import auction_client.AuctionUpdateListener;
import auction_client.Network.ClientService;
import auction_client.UserSession;
import auction_shared.dto.UserDTO;
import auction_shared.Network.NetworkMessage;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;


public class SignInController implements Initializable, AuctionUpdateListener {
    @FXML
    public TextField username;
    @FXML
    public PasswordField password;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ClientService.getInstance().addListener(this);
    }

    @FXML
    public void onSignInClicked(ActionEvent event) {
        String inputUsername = username.getText().trim();
        String inputPassword = password.getText();

        if (inputUsername.isEmpty() || inputPassword.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập đầy đủ tài khoản và mật khẩu!", Alert.AlertType.WARNING);
            return;
        }

        UserDTO user = new UserDTO("id", inputUsername);
        UserSession.getInstance().setUser(user);
        NetworkMessage msg = new NetworkMessage("LOGIN", user);
        ClientService.getInstance().sendMessage(msg);

    }

    @Override
    public void onUpdateReceived(NetworkMessage msg) {
        if ("LOGIN".equals(msg.getAction())) {
            Boolean isSuccess = (Boolean) msg.getData();

            Platform.runLater(() -> {
                if (isSuccess) {
                    UserSession.getInstance().setUsername(username.getText().trim());
                    switchToMainScene();
                } else {
                    UserSession.getInstance().setUser(null);
                    showAlert("Thất bại", "Tài khoản hoặc mật khẩu không chính xác!", Alert.AlertType.ERROR);
                }
            });
        }
    }

    private void switchToMainScene() {
        try {
            ClientService.getInstance().removeListener(this);

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/auction_client/AuctionMain.fxml"));
            Parent root = fxmlLoader.load();

            // Lấy Stage hiện tại thông qua một node bất kỳ (ví dụ: username TextField)
            Stage stage = (Stage) username.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.centerOnScreen();
            stage.setMaximized(true);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi Hệ Thống", "Không thể tải giao diện chính.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void switchToSignUpScene(ActionEvent event) {
        try {
            ClientService.getInstance().removeListener(this);

            Parent root = FXMLLoader.load(getClass().getResource("/auction_client/SignUpScene.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
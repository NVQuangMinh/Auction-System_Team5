package auctionclient.controllers.auth;

import auctionclient.interfaces.AuctionUpdateListener;
import auctionclient.Network.ClientService;
import auctionclient.UserSession;
import auctionshared.dto.SignUpDTO;
import auctionshared.dto.UserDTO;
import auctionshared.Network.NetworkMessage;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
    public static final BooleanProperty isAdmin =  new SimpleBooleanProperty(false);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ClientService.getInstance().addListener(this);
    }

    /**
     * Xử lí khi người dùng ấn vào nút đăng nhập
     *
     * Kiểm tra nếu tên đăng nhập và mật khẩu trống thì cảnh báo
     * Nếu không lỗi thì khởi tạo SignUpDTO gửi dữ liệu người dùng cùng tin nhắn "LOGIN" cho ClientService
     */
    @FXML
    public void onSignInClicked() {
        String inputUsername = username.getText().trim();
        String inputPassword = password.getText();

        if (inputUsername.isEmpty() || inputPassword.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập đầy đủ tài khoản và mật khẩu!", Alert.AlertType.WARNING);
            return;
        }

        SignUpDTO loginData = new SignUpDTO(null, inputUsername, inputPassword);
        ClientService.getInstance().sendMessage(new NetworkMessage("LOGIN", loginData));

    }

    @Override
    public void onUpdateReceived(NetworkMessage msg) {
        if ("LOGIN".equals(msg.getAction())) {
            UserDTO user = (UserDTO) msg.getData();
            Platform.runLater(() -> {
                if (user != null) {
                    UserSession.getInstance().setUsername(username.getText().trim());
                    UserSession.getInstance().setUser(user);
                    boolean roleIsAdmin = "ADMIN".equalsIgnoreCase(UserSession.getInstance().getUser().getRole());
                    SignInController.isAdmin.set(roleIsAdmin);
                    switchToMainScene();
                } else {
                    UserSession.getInstance().setUser(null);
                    showAlert("Đăng nhập thất bại", "Tài khoản hoặc mật khẩu không chính xác hoặc bạn đã bị ban !", Alert.AlertType.ERROR);
                }
            });
        }
    }

    /**
     * Vào trang chủ
     *
     * Chuyển từ cửa sổ SignIn vào trang chủ AuctionMain
     * @exception IOException khi không load được trang chủ thì báo lỗi
     */
    private void switchToMainScene() {
        try {
            ClientService.getInstance().removeListener(this);

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/auctionclient/AuctionMain.fxml"));
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

            Parent root = FXMLLoader.load(getClass().getResource("/auctionclient/SignUpScene.fxml"));
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
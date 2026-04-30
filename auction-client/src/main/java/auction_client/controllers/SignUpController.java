package auction_client.controllers;

import auction_client.AuctionUpdateListener;
import auction_client.Network.ClientService;
import auction_client.UserSession;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.SignUpDTO;
import auction_shared.dto.UserDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SignUpController implements Initializable, AuctionUpdateListener {
    @FXML
    public PasswordField password;
    @FXML
    public PasswordField confirmpassword;
    @FXML
    public TextField username;

    private String inputUsername;
    private String inputPassword;

    Stage stage;
    Parent root;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ClientService.getInstance().addListener(this);
    }

    @Override
    public void onUpdateReceived(NetworkMessage msg) {
        String action = (String) msg.getAction();
        if (action.equals("CREATE_ACCOUNT")){
            Boolean isSuccess = (Boolean) msg.getData();
            Platform.runLater(() ->{
                if (isSuccess){
                    UserSession.getInstance().setUsername(inputUsername);
                    switchToMainScene();
                }
                else{
                    UserSession.getInstance().setUser(null);
                    showAlert(Alert.AlertType.ERROR,"Register failed","Unable to sign up");
                }
            });
        }
    }

    @FXML
    public void onSignUpClicked(){
        inputUsername = username.getText().trim();
        inputPassword = password.getText();
        String inputPasswordConfirm = confirmpassword.getText();
        if (!inputUsername.isEmpty() && !inputPassword.isEmpty() && !inputPasswordConfirm.isEmpty()) {
            if (!inputPassword.equals(inputPasswordConfirm)) {
                showAlert(Alert.AlertType.ERROR, "Lỗi Mật Khẩu", "Mật khẩu xác nhận không khớp. Vui lòng nhập lại!");
                confirmpassword.clear();
                return;
            }

            SignUpDTO request = new SignUpDTO("id",inputUsername,inputPassword);
            UserSession.getInstance().setUser(new UserDTO("id", inputUsername));
            ClientService.getInstance().sendMessage(new NetworkMessage("CREATE_ACCOUNT", request));
        }
    }

    @FXML
    public void switchToMainScene() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/auction_client/AuctionMain.fxml"));
            root = fxmlLoader.load();

            stage = (Stage) username.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.centerOnScreen();
            stage.setMaximized(true);
            stage.show();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void switchToSignUpScene() throws IOException {
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
package auctionclient.controllers.main;

import auctionclient.Network.ClientService;
import auctionclient.UserSession;
import auctionclient.controllers.auth.SignInController;
import auctionclient.interfaces.Cleanable;
import auctionshared.Network.NetworkMessage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;

//import javafx.scene.control.*;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class WebMenuBarController implements Initializable {
    @FXML
    public Label welcome;
    @FXML
    public Button productsMenuButton;
    @FXML
    public Button userProductListButton;
    @FXML
    public Button adminControlPanelButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        adminControlPanelButton.managedProperty().bind(adminControlPanelButton.visibleProperty());
        productsMenuButton.managedProperty().bind(productsMenuButton.visibleProperty());
        userProductListButton.managedProperty().bind(userProductListButton.visibleProperty());
        adminControlPanelButton.visibleProperty().bind(SignInController.isAdmin);
        productsMenuButton.visibleProperty().bind(SignInController.isAdmin.not());
        userProductListButton.visibleProperty().bind(SignInController.isAdmin.not());

        setWelcomeUsername(UserSession.getInstance().getUsername());
    }

    public void setWelcomeUsername(String username) {
        if (username != null && !username.isBlank()) {
            welcome.setText(username.trim());
        }
    }

    @FXML
    public void switchToMainScene(MouseEvent event) throws IOException {
        cleanupCurrentScene(event);
        switchScene(event, "/auctionclient/AuctionMain.fxml");
    }

    @FXML
    public void switchToUserProductListScene(ActionEvent event) throws IOException {
        cleanupCurrentScene(event);
        ClientService.getInstance()
                .sendMessage(new NetworkMessage("GET_MY_LIST", UserSession.getInstance().getUsername()));
        switchScene(event, "/auctionclient/SellProductScene.fxml");
    }

    @FXML
    public void switchToAllProductScene(ActionEvent event) throws IOException {
        cleanupCurrentScene(event);
        switchScene(event, "/auctionclient/AllProductScene.fxml");
    }

    @FXML
    public void switchToActivitiesScene(ActionEvent event) throws IOException {
        cleanupCurrentScene(event);
        switchScene(event, "/auctionclient/ActivitiesScene.fxml");
    }

    @FXML
    public void switchToAdminControlPanel(ActionEvent event) throws IOException {
        cleanupCurrentScene(event);
        switchScene(event, "/auctionclient/AdminControlPanel.fxml");
    }

    private void cleanupCurrentScene(javafx.event.Event event) {
        // Lấy ra nút giao diện (Node) đã kích hoạt sự kiện
        Node source = (Node) event.getSource();
        // Lấy root - màn hình trước đó ==> màn hình hiện tại
        Parent root = source.getScene().getRoot();
        // Lấy controller và xoá bớt
        Object controller = root.getProperties().get("fx_controller");
        if (controller instanceof Cleanable cleanable) {
            cleanable.cleanup();
        }
    }

    private void switchScene(javafx.event.Event event, String fxmlPath) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = fxmlLoader.load();
        root.getProperties().put("fx_controller", fxmlLoader.getController());
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
        stage.centerOnScreen();
        stage.show();
    }

    @FXML
    public void logOut(MouseEvent event) throws IOException {
        Alert logOutAlert = new Alert(Alert.AlertType.CONFIRMATION);
        logOutAlert.setTitle("Đăng xuất");
        logOutAlert.setHeaderText("Chuyển hướng tới trang đăng nhập");
        logOutAlert.setContentText("Bạn có chắc chắn muốn đăng xuất?");

        if (logOutAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            cleanupCurrentScene(event);
            UserSession.getInstance().closeApp();
            switchScene(event, "/auctionclient/SignInScene.fxml");
        }
    }
}

package auction_system.controllers;

import auction_system.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
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
    public ImageView logoutImage;
    @FXML
    public Button productsButton;

    public void initialize(URL url, ResourceBundle resourceBundle) {
        setWelcomeUsername(UserSession.getInstance().getUsername());
    }
    public void setWelcomeUsername(String username) {
        if (username != null && !username.isBlank()) {
            welcome.setText("Welcome, " + "\n" + username.trim());
        }
    }

    @FXML
    public void switchToMainScene(MouseEvent event) throws IOException {
        switchScene(event, "/auction_system/AuctionMain.fxml");
    }

    @FXML
    public void switchToUserProductListScene(ActionEvent event) throws IOException {
        switchScene(event, "/auction_system/UserProductList.fxml");
    }

    @FXML
    public void switchToProductScene(ActionEvent event) throws IOException {
        switchScene(event, "/auction_system/ProductScene.fxml");
    }

    @FXML
    public void switchToActivitiesScene(ActionEvent event) throws IOException {
        switchScene(event,"/auction_system/ActivitiesScene.fxml");
    }

    private void switchScene(javafx.event.Event event, String fxmlPath) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = fxmlLoader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
        stage.centerOnScreen();
        stage.show();
    }

    @FXML
    public void logOut(MouseEvent event) throws IOException {
        Alert logOutAlert = new Alert(Alert.AlertType.CONFIRMATION);
        logOutAlert.setTitle("Logout");
        logOutAlert.setHeaderText("Redirecting to sign-in page...");
        logOutAlert.setContentText("Are you sure you want to logout?");

        if (logOutAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            switchScene(event, "/auction_system/SignInScene.fxml");
        }
    }



}

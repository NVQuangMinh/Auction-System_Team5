package auction_client.controllers;

import auction_client.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
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
    public MenuButton productsMenuButton; // Đã sửa từ Button sang MenuButton


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle){
        setWelcomeUsername(UserSession.getInstance().getUsername());
        // Lắng nghe sự kiện khi MenuButton hiển thị menu thả xuống
        productsMenuButton.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing) {
                double width = productsMenuButton.getWidth();
                var contextMenu = productsMenuButton.getContextMenu();

                if (contextMenu != null) {
                    // Ép kích thước menu con bằng đúng kích thước nút cha
                    contextMenu.setMinWidth(width);
                    contextMenu.setPrefWidth(width);
                    contextMenu.setMaxWidth(width);
                }
            }
        });
    }
    public void setWelcomeUsername(String username) {
        if (username != null && !username.isBlank()) {
            welcome.setText("Welcome, " + "\n" + username.trim());
        }
    }

    @FXML
    public void switchToMainScene(MouseEvent event) throws IOException {
        switchScene(event, "/auction_client/AuctionMain.fxml");
    }

    @FXML
    public void switchToUserProductListScene(ActionEvent event) throws IOException {
        switchScene(event, "/auction_client/SellProductScene.fxml");
    }

    @FXML
    public void switchToProductScene(ActionEvent event) throws IOException {
        switchScene(event, "/auction_client/BidProductScene.fxml");
    }

    @FXML
    public void switchToActivitiesScene(ActionEvent event) throws IOException {
        switchScene(event,"/auction_client/ActivitiesScene.fxml");
    }

    @FXML
    public void switchToArtScene(ActionEvent event) throws IOException {
        switchScene(event, "/auction_client/ArtScene.fxml");
    }

    @FXML
    public void switchToElectronicScene(ActionEvent event) throws IOException {
        switchScene(event, "/auction_client/ElectronicScene.fxml");
    }

    @FXML
    public void switchToVehicleScene(ActionEvent event) throws IOException {
        switchScene(event, "/auction_client/VehicleScene.fxml");
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
            switchScene(event, "/auction_client/SignInScene.fxml");
        }
    }



}

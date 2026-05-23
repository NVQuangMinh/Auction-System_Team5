package auction_client.controllers;

import auction_client.Network.ClientService;
import auction_client.UserSession;
import auction_shared.Network.NetworkMessage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
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
    public Button productsMenuButton;
    @FXML
    public Button userProductListButton;
    @FXML
    public Button adminControlPanelButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle){
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
        switchScene(event, "/auction_client/AuctionMain.fxml");
    }

    @FXML
    public void switchToUserProductListScene(ActionEvent event) throws IOException {
        cleanupCurrentScene(event);
        ClientService.getInstance().sendMessage(new NetworkMessage("GET_MY_LIST", UserSession.getInstance().getUsername()));
        switchScene(event, "/auction_client/SellProductScene.fxml");
    }

    @FXML
    public void switchToAllProductScene(ActionEvent event) throws IOException {
        cleanupCurrentScene(event);
        switchScene(event, "/auction_client/AllProductScene.fxml");
    }

    @FXML
    public void switchToActivitiesScene(ActionEvent event) throws IOException {
        cleanupCurrentScene(event);
        switchScene(event, "/auction_client/ActivitiesScene.fxml");
    }

    @FXML
    public void switchToAdminControlPanel(ActionEvent event) throws IOException {
        cleanupCurrentScene(event);
        switchScene(event, "/auction_client/AdminControlPanel.fxml");
    }

    /**
     * Retrieves the controller of the current scene's root BorderPane
     * and calls cleanup() if it implements the cleanup interface.
     */
    private void cleanupCurrentScene(javafx.event.Event event) {
        Node source = (Node) event.getSource();
        Parent root = source.getScene().getRoot();
        Object controller = root.getProperties().get("fx_controller");
        if (controller instanceof AuctionMainController mainController) {
            mainController.cleanup();
        } else if (controller instanceof AllProductController allProductController) {
            allProductController.cleanup();
        } else if (controller instanceof BidProductSceneController bidController) {
            bidController.cleanup();
        } else if (controller instanceof FilteredProductSceneController filteredController) {
            filteredController.cleanup();
        } else if (controller instanceof SellProductSceneController sellController) {
            sellController.cleanup();
        }
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
            cleanupCurrentScene(event);
            UserSession.getInstance().closeApp();
            switchScene(event, "/auction_client/SignInScene.fxml");
        }
    }
}

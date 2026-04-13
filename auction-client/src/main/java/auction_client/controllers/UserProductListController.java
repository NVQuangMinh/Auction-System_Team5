package auction_client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class UserProductListController {
    @FXML
    public AnchorPane overlayPane;
    @FXML
    private WebMenuBarController menuBarController;

    public WebMenuBarController getMenuBarController() {
        return menuBarController;
    }

    @FXML
    public void handleUserAddItem(ActionEvent event) throws IOException{
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/auction_client/ProductInfoSubmission.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        Stage owner = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.initOwner(owner);
        stage.setOnHiding(e -> overlayPane.setVisible(false));
        stage.setScene(scene);
        stage.setTitle("Add New Product");
        overlayPane.setVisible(true);
        stage.show();

    }
}

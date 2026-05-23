package auction_client.controllers;

import javafx.fxml.FXML;

public class AuctionMainController {
    @FXML
    private WebMenuBarController menuBarController;
    @FXML
    private AllProductController allProductSceneController;

    public WebMenuBarController getMenuBarController() {
        return menuBarController;
    }

    public AllProductController getAllProductSceneController() {
        return allProductSceneController;
    }

    public void cleanup() {
        if (allProductSceneController != null) {
            allProductSceneController.cleanup();
        }
    }
}

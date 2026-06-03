package auctionclient.controllers.main;

import auctionclient.controllers.bidder.AllProductController;
import auctionclient.interfaces.Cleanable;
import javafx.fxml.FXML;

public class AuctionMainController implements Cleanable {
    @FXML
    private WebMenuBarController menuBarController;
    @FXML
    private AllProductController allProductSceneController;

    public WebMenuBarController getMenuBarController() {
        return menuBarController;
    }


    @Override
    public void cleanup() {
        if (allProductSceneController != null) {
            allProductSceneController.cleanup();
        }
    }
}

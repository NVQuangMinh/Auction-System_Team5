package auction_client.controllers.main;

import auction_client.controllers.bidder.AllProductController;
import auction_client.interfaces.Cleanable;
import javafx.fxml.FXML;

public class AuctionMainController implements Cleanable {
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

    @Override
    public void cleanup() {
        if (allProductSceneController != null) {
            allProductSceneController.cleanup();
        }
    }
}

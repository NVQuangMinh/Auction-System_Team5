package auction_client.controllers;

import javafx.fxml.FXML;

public class AuctionMainController {
    @FXML
    // Phải đặt tên là menuBarController để khớp với fx:id="menuBar" trong FXML
    private WebMenuBarController menuBarController;

    public WebMenuBarController getMenuBarController(){
        return menuBarController;
    }
}

module auction.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires auction.shared;

    exports auction_client.launcher;
    exports auction_client.controllers;
    opens auction_client.launcher to javafx.graphics, javafx.fxml;
    opens auction_client.controllers to javafx.fxml, javafx.graphics;
    opens auction_client.Network to auction.shared;
    opens auction_client to javafx.fxml;

}
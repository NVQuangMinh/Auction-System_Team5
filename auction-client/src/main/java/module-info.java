module auction.client {
  requires javafx.controls;
  requires javafx.fxml;
  requires java.desktop;
  requires auction.shared;
  requires org.slf4j;
  requires ch.qos.logback.classic;
  requires org.kordamp.ikonli.core;
  requires org.kordamp.ikonli.javafx;
  requires org.kordamp.ikonli.fontawesome5;
  uses org.kordamp.ikonli.IkonHandler;

  exports auction_client.launcher;

  opens auction_client.launcher to javafx.graphics, javafx.fxml;
  opens auction_client.Network;
  opens auction_client to javafx.fxml;
  opens auction_client.interfaces to javafx.fxml;
    exports auction_client.controllers.admin;
    opens auction_client.controllers.admin;
  exports auction_client.controllers.auth;
  opens auction_client.controllers.auth;
  exports auction_client.controllers.bidder;
  opens auction_client.controllers.bidder;
  exports auction_client.controllers.seller;
  opens auction_client.controllers.seller;
  exports auction_client.controllers.main;
  opens auction_client.controllers.main;
  exports auction_client.controllers.notification;
  opens auction_client.controllers.notification;

}
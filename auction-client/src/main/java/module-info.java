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

  exports auctionclient.launcher;

  opens auctionclient.launcher to javafx.graphics, javafx.fxml;
  opens auctionclient.Network;
  opens auctionclient to javafx.fxml;
  opens auctionclient.interfaces to javafx.fxml;
    exports auctionclient.controllers.admin;
    opens auctionclient.controllers.admin;
  exports auctionclient.controllers.auth;
  opens auctionclient.controllers.auth;
  exports auctionclient.controllers.bidder;
  opens auctionclient.controllers.bidder;
  exports auctionclient.controllers.seller;
  opens auctionclient.controllers.seller;
  exports auctionclient.controllers.main;
  opens auctionclient.controllers.main;
  exports auctionclient.controllers.notification;
  opens auctionclient.controllers.notification;

}
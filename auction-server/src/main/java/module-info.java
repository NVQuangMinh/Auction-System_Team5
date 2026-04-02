module project.auction_system_team5 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens project.auction_system_team5 to javafx.fxml;
    exports project.auction_system_team5;
    exports project.auction_system_team5.entity;
    opens project.auction_system_team5.entity to javafx.fxml;
    exports project.auction_system_team5.controller;
    opens project.auction_system_team5.controller to javafx.fxml;
}
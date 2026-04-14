module project.auction_system_team5 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    // Cho phép JavaFX truy cập vào class khởi chạy (Launcher, entity.Auction)
    opens auction_system.launcher to javafx.graphics, javafx.fxml;

    // Cho phép JavaFX FXML load các Controller
    opens auction_system.controllers to javafx.fxml;

    // Xuất các package để các module khác có thể dùng (nếu cần)
    exports auction_system.launcher;
    exports auction_system.controllers;
    exports auction_client.entity;
    opens auction_client.entity to javafx.fxml;
}
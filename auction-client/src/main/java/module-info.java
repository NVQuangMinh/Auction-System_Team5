module auction.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    // Cho phép JavaFX truy cập vào class khởi chạy (Launcher, ClientLauncher)
    opens auction_client.launcher to javafx.graphics, javafx.fxml;

    // Cho phép JavaFX FXML load các Controller

    // Xuất các package để các module khác có thể dùng (nếu cần)
    exports auction_client.launcher;
    exports auction_client.controllers;
    opens auction_client.controllers to javafx.fxml, javafx.graphics;
}
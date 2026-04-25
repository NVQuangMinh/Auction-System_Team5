package auction_client.launcher;

import auction_client.Network.ClientService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientLauncher extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Kết nối đến server trước khi load UI
        try {
            ClientService.getInstance().connect("localhost", 8080);
        } catch (IOException e) {
            System.err.println("Could not connect to server: " + e.getMessage());
            // Tùy chọn: Hiển thị thông báo lỗi cho người dùng hoặc dừng ứng dụng
        }

        FXMLLoader fxmlLoader = new FXMLLoader(ClientLauncher.class.getResource("/auction_client/SignInScene.fxml"));
        stage.setTitle("The ClientLauncher Studio");
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }
}

package auction_client.launcher;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientLauncher extends Application {
    @Override
    public void start(Stage stage) throws IOException {

        // Sửa lỗi sai tên folder tài nguyên từ auction_system sang auction_client
        FXMLLoader fxmlLoader = new FXMLLoader(ClientLauncher.class.getResource("/auction_client/SignInScene.fxml"));
        stage.setTitle("The ClientLauncher Studio");
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }
}

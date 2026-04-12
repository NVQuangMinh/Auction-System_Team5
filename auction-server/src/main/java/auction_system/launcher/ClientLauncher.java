package auction_system.launcher;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientLauncher extends Application {
    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(ClientLauncher.class.getResource("/auction_system/SignInScene.fxml"));
        stage.setTitle("The Auction Studio");
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }
}


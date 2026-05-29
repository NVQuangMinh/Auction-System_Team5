package auctionclient.launcher;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientLauncher extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ClientLauncher.class.getResource("/auctionclient/SignInScene.fxml"));
        stage.setTitle("The ClientLauncher Studio");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/auctionclient/images/Auction-System.png")));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }
}

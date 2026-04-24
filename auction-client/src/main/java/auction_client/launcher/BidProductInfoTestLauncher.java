package auction_client.launcher;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class BidProductInfoTestLauncher extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Đường dẫn đến FXML file của bạn
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/auction_client/BidProductInfo.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Bid Product Info Test"); // Đặt tiêu đề cho cửa sổ test
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
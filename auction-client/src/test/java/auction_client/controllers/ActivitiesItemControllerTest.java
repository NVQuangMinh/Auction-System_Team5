package auction_client.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.matcher.control.LabeledMatchers;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ActivitiesItemControllerTest extends ApplicationTest{
    private ActivitiesItemController controller;
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/ActivitiesItem.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    public void testSetData(){
        String testNotification = "You have placed bid successfully";
        String testTime = "12:00 AM";

        interact(() -> {
            controller.setData(testNotification, testTime);
        });

        FxAssert.verifyThat("#notification", LabeledMatchers.hasText(testNotification));
        FxAssert.verifyThat("#notificationTime", LabeledMatchers.hasText(testTime));
    }
}
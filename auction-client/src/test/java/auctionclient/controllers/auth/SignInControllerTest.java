package auctionclient.controllers.auth;

import auctionclient.Network.ClientService;
import auctionclient.UserSession;
import auctionshared.Network.NetworkMessage;
import auctionshared.dto.SignUpDTO;
import auctionshared.dto.UserDTO;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SignInControllerTest extends ApplicationTest {

    private SignInController controller;
    private ClientService mockClientService;

    private final UserDTO testUser = new UserDTO("67", "nhan", "USER");

    @AfterEach
    public void closeMock() {
        try {
            java.lang.reflect.Field instanceField = ClientService.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @BeforeEach
    public void resetSession() {
        interact(() -> {
            UserSession.getInstance().setUser(null);
            UserSession.getInstance().setUsername("");
            SignInController.isAdmin.set(false);
            lookup("#username").queryAs(TextField.class).clear();
            lookup("#password").queryAs(PasswordField.class).clear();
        });
    }

    @Override
    public void start(Stage stage) throws Exception {
        mockClientService = mock(ClientService.class);
        java.lang.reflect.Field instanceField = ClientService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, mockClientService);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/auctionclient/SignInScene.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    public void testSignIn_EmptyFields() {
        scheduleAlertDismissal();

        interact(() -> controller.onSignInClicked());

        verify(mockClientService, never()).sendMessage(any());
    }

    @Test
    public void testSignIn_WrongCredentials() {
        scheduleAlertDismissal();

        interact(() -> {
            lookup("#username").queryAs(TextField.class).setText("wrong");
            lookup("#password").queryAs(PasswordField.class).setText("wrong");
            controller.onSignInClicked();
        });

        controller.onUpdateReceived(new NetworkMessage("LOGIN", null));
        WaitForAsyncUtils.waitForFxEvents();

        assertNull(UserSession.getInstance().getUser());
        verify(mockClientService, never()).removeListener(controller);
    }

    @Test
    public void testSignIn_Success() {
        interact(() -> {
            lookup("#username").queryAs(TextField.class).setText("nhan");
            lookup("#password").queryAs(PasswordField.class).setText("secret123");
            controller.onSignInClicked();
        });

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService).sendMessage(captor.capture());
        assertEquals("LOGIN", captor.getValue().getAction());
        SignUpDTO loginData = (SignUpDTO) captor.getValue().getData();
        assertEquals("nhan", loginData.getUsername());
        assertEquals("secret123", loginData.getPassword());

        controller.onUpdateReceived(new NetworkMessage("LOGIN", testUser));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("nhan", UserSession.getInstance().getUsername());
        assertEquals(testUser, UserSession.getInstance().getUser());
        assertFalse(SignInController.isAdmin.get());
        verify(mockClientService).removeListener(controller);

        BorderPane mainPane = lookup("#MainPane").queryAs(BorderPane.class);
        assertNotNull(mainPane);
    }

    private void scheduleAlertDismissal() {
        Thread dismissThread = new Thread(() -> {
            try {
                Thread.sleep(500);
                Platform.runLater(() -> {
                    try {
                        if (lookup(".dialog-pane").tryQuery().isPresent()) {
                            clickOn("OK");
                        }
                    } catch (Exception ignored) {
                        // Alert may already be closed
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        dismissThread.setDaemon(true);
        dismissThread.start();
    }
}

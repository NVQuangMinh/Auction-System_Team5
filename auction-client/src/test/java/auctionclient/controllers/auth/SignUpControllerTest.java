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
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import auctionclient.controllers.FxControllerTestBase;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SignUpControllerTest extends FxControllerTestBase {

    private static final String TEST_USERNAME = "newuser";
    private static final String TEST_PASSWORD = "secret123";

    private SignUpController controller;
    private ClientService mockClientService;

    @Override
    public void start(Stage stage) throws Exception {
        mockClientService = mock(ClientService.class);
        java.lang.reflect.Field instanceField = ClientService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, mockClientService);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/auctionclient/SignUpScene.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        stage.setScene(new Scene(root));
        stage.show();
    }

    @BeforeEach
    void resetFormAndSession() {
        interact(() -> {
            UserSession.getInstance().setUser(null);
            UserSession.getInstance().setUsername("");
            lookup("#username").queryAs(TextField.class).clear();
            lookup("#password").queryAs(PasswordField.class).clear();
            lookup("#confirmpassword").queryAs(PasswordField.class).clear();
        });
    }

    @AfterEach
    void closeMock() {
        try {
            java.lang.reflect.Field instanceField = ClientService.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testSignUp_ValidCredentials() {
        fillSignUpForm(TEST_USERNAME, TEST_PASSWORD, TEST_PASSWORD);

        interact(() -> controller.onSignUpClicked());

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService).sendMessage(captor.capture());

        assertEquals("CREATE_ACCOUNT", captor.getValue().getAction());
        SignUpDTO request = (SignUpDTO) captor.getValue().getData();
        assertNotNull(request.getId());
        assertEquals(TEST_USERNAME, request.getUsername());
        assertEquals(TEST_PASSWORD, request.getPassword());

        UserDTO sessionUser = UserSession.getInstance().getUser();
        assertNotNull(sessionUser);
        assertEquals(TEST_USERNAME, sessionUser.getUsername());
        assertEquals(request.getId(), sessionUser.getId());
        assertEquals("USER", sessionUser.getRole());

        controller.onUpdateReceived(new NetworkMessage("CREATE_ACCOUNT", true));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(TEST_USERNAME, UserSession.getInstance().getUsername());
        assertNotNull(controller.root);
    }

    @Test
    void testSignUp_PasswordMismatch() {
        scheduleAlertDismissal();

        fillSignUpForm(TEST_USERNAME, TEST_PASSWORD, "different");

        interact(() -> controller.onSignUpClicked());
        WaitForAsyncUtils.waitForFxEvents();

        verify(mockClientService, never()).sendMessage(any());
        assertNull(UserSession.getInstance().getUser());

        PasswordField confirmField = lookup("#confirmpassword").queryAs(PasswordField.class);
        assertEquals("", confirmField.getText());
    }

    @Test
    void testSignUp_UserAlreadyExists() {
        scheduleAlertDismissal();

        fillSignUpForm(TEST_USERNAME, TEST_PASSWORD, TEST_PASSWORD);

        interact(() -> controller.onSignUpClicked());

        verify(mockClientService).sendMessage(any());

        controller.onUpdateReceived(new NetworkMessage("CREATE_ACCOUNT", false));
        WaitForAsyncUtils.waitForFxEvents();

        assertNull(UserSession.getInstance().getUser());
    }

    private void fillSignUpForm(String name, String password, String confirmPassword) {
        interact(() -> {
            lookup("#username").queryAs(TextField.class).setText(name);
            lookup("#password").queryAs(PasswordField.class).setText(password);
            lookup("#confirmpassword").queryAs(PasswordField.class).setText(confirmPassword);
        });
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

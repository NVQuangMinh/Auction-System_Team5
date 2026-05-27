package auction_client.controllers.auth;

import auction_client.Network.ClientService;
import auction_client.UserSession;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.SignUpDTO;
import auction_shared.dto.UserDTO;
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
import org.mockito.MockedStatic;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SignUpControllerTest extends ApplicationTest {

    private static final String TEST_USERNAME = "newuser";
    private static final String TEST_PASSWORD = "secret123";

    private SignUpController controller;
    private ClientService mockClientService;
    private MockedStatic<ClientService> mockedStaticClientService;

    @Override
    public void start(Stage stage) throws Exception {
        mockClientService = mock(ClientService.class);
        mockedStaticClientService = mockStatic(ClientService.class);
        mockedStaticClientService.when(() -> ClientService.getInstance()).thenReturn(mockClientService);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/SignUpScene.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        stage.setScene(new Scene(root));
        stage.show();
    }

    @BeforeEach
    void resetUserSession() {
        interact(() -> {
            UserSession.getInstance().setUser(null);
            UserSession.getInstance().setUsername("");
        });
    }

    @AfterEach
    public void closeMock() {
        if (mockedStaticClientService != null) {
            mockedStaticClientService.close();
        }
    }

    @Test
    void testInitialize() {
        verify(mockClientService).addListener(controller);
    }

    @Test
    void testOnSignUpClicked_ValidInput() {
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
    }

    @Test
    void testOnSignUpClicked_PasswordMismatch() {
        scheduleAlertDismissal();

        fillSignUpForm(TEST_USERNAME, TEST_PASSWORD, "different");

        interact(() -> controller.onSignUpClicked());
        WaitForAsyncUtils.waitForFxEvents();

        verify(mockClientService, never()).sendMessage(any());

        PasswordField confirmField = lookup("#confirmpassword").queryAs(PasswordField.class);
        assertEquals("", confirmField.getText());
    }

    @Test
    void testOnSignUpClicked_EmptyFields() {
        interact(() -> controller.onSignUpClicked());

        verify(mockClientService, never()).sendMessage(any());
        assertNull(UserSession.getInstance().getUser());
    }

    @Test
    void testOnSignUpClicked_TrimsUsername() {
        fillSignUpForm("  " + TEST_USERNAME + "  ", TEST_PASSWORD, TEST_PASSWORD);

        interact(() -> controller.onSignUpClicked());

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService).sendMessage(captor.capture());

        SignUpDTO request = (SignUpDTO) captor.getValue().getData();
        assertEquals(TEST_USERNAME, request.getUsername());
        assertEquals(TEST_USERNAME, UserSession.getInstance().getUser().getUsername());
    }

    @Test
    void testOnUpdateReceived_CreateAccountSuccess() {
        fillSignUpForm(TEST_USERNAME, TEST_PASSWORD, TEST_PASSWORD);
        interact(() -> controller.onSignUpClicked());

        NetworkMessage msg = new NetworkMessage("CREATE_ACCOUNT", true);
        controller.onUpdateReceived(msg);
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(TEST_USERNAME, UserSession.getInstance().getUsername());
        assertNotNull(controller.root);
    }

    @Test
    void testOnUpdateReceived_CreateAccountFailure() {
        fillSignUpForm(TEST_USERNAME, TEST_PASSWORD, TEST_PASSWORD);
        interact(() -> controller.onSignUpClicked());

        scheduleAlertDismissal();

        NetworkMessage msg = new NetworkMessage("CREATE_ACCOUNT", false);
        controller.onUpdateReceived(msg);
        WaitForAsyncUtils.waitForFxEvents();

        assertNull(UserSession.getInstance().getUser());
    }

    @Test
    void testOnUpdateReceived_IgnoresOtherActions() {
        fillSignUpForm(TEST_USERNAME, TEST_PASSWORD, TEST_PASSWORD);
        interact(() -> controller.onSignUpClicked());

        UserDTO userBefore = UserSession.getInstance().getUser();

        NetworkMessage msg = new NetworkMessage("OTHER_ACTION", true);
        controller.onUpdateReceived(msg);
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(userBefore, UserSession.getInstance().getUser());
        assertEquals("", UserSession.getInstance().getUsername());
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

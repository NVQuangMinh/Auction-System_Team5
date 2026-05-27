package auction_client.controllers.main;

import auction_client.Network.ClientService;
import auction_client.UserSession;
import auction_client.controllers.auth.SignInController;
import auction_client.controllers.bidder.AllProductController;
import auction_shared.Network.NetworkMessage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.matcher.control.LabeledMatchers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class WebMenuBarControllerTest extends ApplicationTest {

    private static final String TEST_USERNAME = "nhan";

    private WebMenuBarController controller;
    private ClientService mockClientService;
    private MockedStatic<ClientService> mockedStaticClientService;
    private Stage testStage;

    @BeforeEach
    void resetSessionAndRole() {
        UserSession.getInstance().setUsername(TEST_USERNAME);
        SignInController.isAdmin.set(false);
    }

    @AfterEach
    public void closeMock() {
        SignInController.isAdmin.set(false);
        if (mockedStaticClientService != null) {
            mockedStaticClientService.close();
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        mockClientService = mock(ClientService.class);
        mockedStaticClientService = mockStatic(ClientService.class);
        mockedStaticClientService.when(() -> ClientService.getInstance()).thenReturn(mockClientService);

        UserSession.getInstance().setUsername(TEST_USERNAME);
        SignInController.isAdmin.set(false);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/WebMenuBar.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        testStage = stage;
        stage.setScene(new Scene(root, 1200, 100));
        stage.show();
    }

    @Test
    public void testInitialize_regularUser() {
        FxAssert.verifyThat("#welcome", LabeledMatchers.hasText(TEST_USERNAME));

        Button productsBtn = lookup("#productsMenuButton").queryAs(Button.class);
        Button adminBtn = lookup("#adminControlPanelButton").queryAs(Button.class);
        Button userListBtn = lookup("#userProductListButton").queryAs(Button.class);

        assertTrue(productsBtn.isVisible());
        assertTrue(userListBtn.isVisible());
        assertFalse(adminBtn.isVisible());
        assertTrue(productsBtn.isManaged());
        assertFalse(adminBtn.isManaged());
    }

    @Test
    public void testInitialize_adminUser() throws Exception {
        SignInController.isAdmin.set(true);
        UserSession.getInstance().setUsername(TEST_USERNAME);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/WebMenuBar.fxml"));
        Parent root = loader.load();
        interact(() -> testStage.getScene().setRoot(root));

        Button productsBtn = lookup("#productsMenuButton").queryAs(Button.class);
        Button adminBtn = lookup("#adminControlPanelButton").queryAs(Button.class);
        Button userListBtn = lookup("#userProductListButton").queryAs(Button.class);

        assertTrue(adminBtn.isVisible());
        assertFalse(productsBtn.isVisible());
        assertFalse(userListBtn.isVisible());
        assertTrue(adminBtn.isManaged());
        assertFalse(productsBtn.isManaged());
    }

    @Test
    public void testSetWelcomeUsername_trimsAndSets() {
        interact(() -> controller.setWelcomeUsername("  alice  "));
        FxAssert.verifyThat("#welcome", LabeledMatchers.hasText("alice"));
    }

    @Test
    public void testSetWelcomeUsername_blankIgnored() {
        Label welcome = lookup("#welcome").queryAs(Label.class);
        String before = welcome.getText();
        interact(() -> controller.setWelcomeUsername("   "));
        assertEquals(before, welcome.getText());
    }

    @Test
    public void testSetWelcomeUsername_nullIgnored() {
        Label welcome = lookup("#welcome").queryAs(Label.class);
        String before = welcome.getText();
        interact(() -> controller.setWelcomeUsername(null));
        assertEquals(before, welcome.getText());
    }

    @Test
    public void testSwitchToAllProductScene() {
        clickOn("#productsMenuButton");
        assertNotNull(lookup("#productFlowPane").query());
    }

    @Test
    public void testSwitchToUserProductListScene() {
        clickOn("#userProductListButton");

        assertNotNull(lookup("#myListFlowPane").query());

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService, atLeastOnce()).sendMessage(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(msg -> "GET_MY_LIST".equals(msg.getAction())
                        && TEST_USERNAME.equals(msg.getData())));
    }

    @Test
    public void testSwitchToActivitiesScene() {
        clickOn("ACTIVITIES");
        assertNotNull(lookup("#notificationContainer").query());
    }

    @Test
    public void testSwitchToMainScene() {
        clickOn(node -> node instanceof ImageView);
        assertNotNull(lookup("#MainPane").query());
    }

    @Test
    public void testSwitchToAdminControlPanel() throws Exception {
        SignInController.isAdmin.set(true);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/WebMenuBar.fxml"));
        Parent root = loader.load();
        interact(() -> testStage.getScene().setRoot(root));

        clickOn("#adminControlPanelButton");
        assertNotNull(lookup("#userTable").query());
    }

    @Test
    public void testCleanupOnNavigationFromAllProducts() {
        clickOn("#productsMenuButton");
        assertNotNull(lookup("#productFlowPane").query());

        AllProductController allProductController = (AllProductController) testStage.getScene()
                .getRoot()
                .getProperties()
                .get("fx_controller");
        assertNotNull(allProductController);

        reset(mockClientService);
        clickOn("#userProductListButton");

        verify(mockClientService).removeListener(allProductController);
        assertNotNull(lookup("#myListFlowPane").query());
    }

    @Test
    public void testLogOut_confirm() {
        clickOn("LOGOUT");
        clickOn("OK");

        verify(mockClientService).sendMessage(argThat(msg -> "LOGOUT".equals(msg.getAction())));
        assertNotNull(lookup("#username").query());
    }

    @Test
    public void testLogOut_cancel() {
        clickOn("LOGOUT");
        clickOn("Cancel");

        verify(mockClientService, never()).sendMessage(argThat(msg -> "LOGOUT".equals(msg.getAction())));
        FxAssert.verifyThat("#welcome", LabeledMatchers.hasText(TEST_USERNAME));
    }
}

package auctionclient.controllers.main;

import auctionclient.Network.ClientService;
import auctionclient.UserSession;
import auctionclient.controllers.auth.SignInController;
import auctionshared.Network.NetworkMessage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.testfx.api.FxAssert;
import auctionclient.controllers.FxControllerTestBase;
import org.testfx.matcher.control.LabeledMatchers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebMenuBarControllerTest extends FxControllerTestBase {

    private static final String TEST_USERNAME = "nhan";

    private ClientService mockClientService;
    private MockedStatic<ClientService> mockedStaticClientService;
    private Stage testStage;

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

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/auctionclient/WebMenuBar.fxml"));
        Parent root = loader.load();

        testStage = stage;
        stage.setScene(new Scene(root, 1200, 100));
        stage.show();
    }

    @Test
    public void testWelcomeDisplaysCurrentUsername() {
        FxAssert.verifyThat("#welcome", LabeledMatchers.hasText(TEST_USERNAME));
    }

    @Test
    public void testProductButtonNavigatesToAllProductPage() {
        clickOn("#productsMenuButton");
        assertNotNull(lookup("#productFlowPane").query());
    }

    @Test
    public void testYourListingsButtonNavigatesToSellProductPage() {
        clickOn("#userProductListButton");
        assertNotNull(lookup("#myListFlowPane").query());

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService, atLeastOnce()).sendMessage(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(msg -> "GET_MY_LIST".equals(msg.getAction())
                        && TEST_USERNAME.equals(msg.getData())));
    }

    @Test
    public void testActivitiesButtonNavigatesToActivitiesPage() {
        clickOn("#activities");
        assertNotNull(lookup("#notificationContainer").query());
    }

    @Test
    public void testMainLogoNavigatesToMainPage() {
        clickOn(node -> node instanceof ImageView);
        assertNotNull(lookup("#MainPane").query());
    }

    @Test
    public void testAdminControlPanelButtonNavigatesToAdminPage() throws Exception {
        SignInController.isAdmin.set(true);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/auctionclient/WebMenuBar.fxml"));
        Parent root = loader.load();
        interact(() -> testStage.getScene().setRoot(root));

        clickOn("#adminControlPanelButton");
        assertNotNull(lookup("#userTable").query());
    }

    @Test
    public void testLogoutButtonCanLogout() {
        clickOn("ĐĂNG XUẤT");
        clickOn("OK");
        assertNotNull(lookup("#username").query());
    }
}

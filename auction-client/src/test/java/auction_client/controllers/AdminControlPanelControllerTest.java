package auction_client.controllers;

import auction_client.Network.ClientService;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.UserDTO;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testfx.framework.junit5.ApplicationTest;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminControlPanelControllerTest extends ApplicationTest {
    private AdminControlPanelController controller;
    private ClientService mockClientService;
    private MockedStatic<ClientService> mockedStaticClientService;

    @BeforeEach
    public void setUp() {
        mockClientService = mock(ClientService.class);

        mockedStaticClientService = mockStatic(ClientService.class);
        mockedStaticClientService.when(() -> ClientService.getInstance()).thenReturn(mockClientService);
    }

    @AfterEach
    public void closeMock(){
        mockedStaticClientService.close();
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_client/AdminControlPanel.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    public void testInitialize() {
        Mockito.verify(mockClientService, times(1)).addListener(controller);
        ArgumentCaptor<NetworkMessage> messageCaptor = ArgumentCaptor.forClass(NetworkMessage.class);
        Mockito.verify(mockClientService, times(2)).sendMessage(messageCaptor.capture());
        List<NetworkMessage> msg = messageCaptor.getAllValues();
        assertEquals("GET_USERS", msg.get(0).getAction());
        assertEquals("GET_PRODUCTS", msg.get(1).getAction());
    }

    @Test
    public void testOnUpdateReceived_UserTable() {
        UserDTO user1 = new UserDTO("1", "nam", "USER");
        UserDTO user2 = new UserDTO("2", "nhan", "ADMIN");
        NetworkMessage msg = new NetworkMessage("GET_USERS", (Serializable) Arrays.asList(user1, user2));

        interact(() -> {
            controller.onUpdateReceived(msg);
        });

        TableView<UserDTO> userTable = lookup("#userTable").queryAs(TableView.class);
        assertEquals(2, userTable.getItems().size());
        assertEquals("1", userTable.getItems().get(0).getId());
        assertEquals("2", userTable.getItems().get(1).getId());
        assertEquals("nam", userTable.getItems().get(0).getUsername());
        assertEquals("nhan", userTable.getItems().get(1).getUsername());
        assertEquals("USER", userTable.getItems().get(0).getRole());
        assertEquals("ADMIN", userTable.getItems().get(1).getRole());
    }

    @Test
    public void testOnUpdateReceived_ProductTable() {
        
    }

    @Test
    void handleBanUser() {
    }

    @Test
    void handleRemoveAuction() {
    }
}
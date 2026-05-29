package auctionclient.controllers.bidder;

import auctionclient.Network.ClientService;
import auctionclient.UserSession;
import auctionclient.interfaces.HandleCardClicked;
import auctionshared.Network.NetworkMessage;
import auctionshared.dto.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.matcher.control.LabeledMatchers;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductCardControllerTest extends ApplicationTest {

    private ProductCardController controller;
    private ClientService mockClientService;
    private MockedStatic<ClientService> mockedStaticClientService;

    private final UserDTO testUser = new UserDTO("01", "testUser", "USER");

    private ItemDTO createItem() {
        return new ItemDTO("item-01", "Laptop Gaming", "Mot chiec laptop gaming cao cap",
                testUser, ItemType.ELECTRONICS, null);
    }

    private AuctionDTO createAuction(AuctionStatus status) {
        return new AuctionDTO(createItem(), status, 0, 2000, 50,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                false, null, 500);
    }

    private void verifyAllLabels(String expectedStatus) {
        FxAssert.verifyThat("#itemName", LabeledMatchers.hasText("Laptop Gaming"));
        FxAssert.verifyThat("#itemState", LabeledMatchers.hasText(expectedStatus));
        FxAssert.verifyThat("#description", LabeledMatchers.hasText("Mot chiec laptop gaming cao cap"));
        FxAssert.verifyThat("#currentPrice", LabeledMatchers.hasText("$500"));
        FxAssert.verifyThat("#buyOutPrice", LabeledMatchers.hasText("$2,000"));
        FxAssert.verifyThat("#typeSpecificLabel", NodeMatchers.isInvisible());
    }

    @AfterEach
    public void closeMock() {
        if (mockedStaticClientService != null) {
            mockedStaticClientService.close();
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        mockClientService = mock(ClientService.class);
        mockedStaticClientService = mockStatic(ClientService.class);
        mockedStaticClientService.when(ClientService::getInstance).thenReturn(mockClientService);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/auctionclient/ProductCard.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    public void testSetData_ActiveProduct_ShowsCorrectUi() {
        AuctionDTO auction = createAuction(AuctionStatus.ACTIVE);
        HandleCardClicked mockListener = mock(HandleCardClicked.class);

        interact(() -> controller.setData(auction, mockListener));

        verifyAllLabels("ACTIVE");
    }

    @Test
    public void testSetData_EndedProduct_ShowsCorrectLabels() {
        AuctionDTO auction = createAuction(AuctionStatus.ENDED);
        HandleCardClicked mockListener = mock(HandleCardClicked.class);

        interact(() -> controller.setData(auction, mockListener));

        verifyAllLabels("ENDED");
    }

    @Test
    public void testSetData_SoldProduct_ShowsCorrectLabels() {
        AuctionDTO auction = createAuction(AuctionStatus.SOLD);
        HandleCardClicked mockListener = mock(HandleCardClicked.class);

        interact(() -> controller.setData(auction, mockListener));

        verifyAllLabels("SOLD");
    }

    @Test
    public void testSetData_BannedProduct_ShowsCorrectLabels() {
        AuctionDTO auction = createAuction(AuctionStatus.BANNED);
        HandleCardClicked mockListener = mock(HandleCardClicked.class);

        interact(() -> controller.setData(auction, mockListener));

        verifyAllLabels("BANNED");
    }

    @Test
    public void testSetData_SetsAuctionAndListenerNotNull() throws Exception {
        AuctionDTO auction = createAuction(AuctionStatus.ACTIVE);
        HandleCardClicked mockListener = mock(HandleCardClicked.class);

        interact(() -> controller.setData(auction, mockListener));

        Field auctionField = ProductCardController.class.getDeclaredField("auction");
        auctionField.setAccessible(true);
        assertNotNull(auctionField.get(controller));
        assertEquals(auction, auctionField.get(controller));

        Field listenerField = ProductCardController.class.getDeclaredField("cardClickedListener");
        listenerField.setAccessible(true);
        assertNotNull(listenerField.get(controller));
        assertEquals(mockListener, listenerField.get(controller));
    }

    @Test
    public void testHandleCardClick_CallsListenerWithAuction() {
        AuctionDTO auction = createAuction(AuctionStatus.ACTIVE);
        HandleCardClicked mockListener = mock(HandleCardClicked.class);

        interact(() -> controller.setData(auction, mockListener));
        interact(() -> controller.handleCardClick());

        verify(mockListener, times(1)).openAuctionDetail(auction);
    }

    @Test
    public void testBuyOut_SendsCorrectMessage() {
        UserSession.getInstance().setUser(testUser);

        AuctionDTO auction = createAuction(AuctionStatus.ACTIVE);
        HandleCardClicked mockListener = mock(HandleCardClicked.class);

        interact(() -> controller.setData(auction, mockListener));
        interact(() -> controller.buyOut());

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService, times(1)).sendMessage(captor.capture());

        NetworkMessage sentMsg = captor.getValue();
        assertEquals("BUY_OUT", sentMsg.getAction());

        BidTransactionDTO transaction = (BidTransactionDTO) sentMsg.getData();
        assertEquals(auction, transaction.getAuction());
        assertEquals(2000.0, transaction.getBidAmount());
        assertEquals(testUser.getId(), transaction.getBidder().getId());
    }
}

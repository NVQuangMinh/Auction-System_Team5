package auctionclient.controllers.seller;

import auctionclient.Network.ClientService;
import auctionclient.UserSession;
import auctionshared.Network.NetworkMessage;
import auctionshared.dto.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.testfx.api.FxAssert;
import auctionclient.controllers.FxControllerTestBase;
import org.testfx.matcher.base.NodeMatchers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductInfoSubmissionControllerTest extends FxControllerTestBase {

    private ProductInfoSubmissionController controller;
    private ClientService mockClientService;
    private MockedStatic<ClientService> mockedStaticClientService;

    private final UserDTO testUser = new UserDTO("01", "testuser", "USER");

    @Override
    public void start(Stage stage) throws Exception {
        mockClientService = mock(ClientService.class);
        mockedStaticClientService = mockStatic(ClientService.class);
        mockedStaticClientService.when(() -> ClientService.getInstance()).thenReturn(mockClientService);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/auctionclient/ProductInfoSubmission.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        stage.setScene(new Scene(root));
        stage.show();
    }

    /**
     * Trước mỗi test: điền sẵn một sản phẩm hợp lệ mặc định.
     * Dữ liệu: Laptop – Electronic – start=100, buyout=200, tick=10, duration=30
     * Các test chỉ cần override đúng trường mình muốn thay đổi.
     */
    @BeforeEach
    @SuppressWarnings("unchecked")
    public void fillDefaultForm() {
        interact(() -> {
            lookup("#productName").queryAs(TextField.class).setText("Laptop");
            lookup("#productDescription").queryAs(javafx.scene.control.TextArea.class).setText("A good laptop");
            lookup("#startingPrice").queryAs(TextField.class).setText("100");
            lookup("#buyoutPrice").queryAs(TextField.class).setText("200");
            lookup("#tickSize").queryAs(TextField.class).setText("10");
            lookup("#bidDurationField").queryAs(TextField.class).setText("30");
            lookup("#types").queryAs(ChoiceBox.class).setValue("Electronic");
        });
    }

    @AfterEach
    public void closeMock() {
        if (mockedStaticClientService != null) {
            mockedStaticClientService.close();
        }
    }

    // =========================================================================
    // 1. testInitialize – khởi tạo: error ẩn, ChoiceBox có đủ 3 options
    // =========================================================================
    @Test
    public void testInitialize() {
        Label error = lookup("#error").queryAs(Label.class);
        assertNotNull(error);
        FxAssert.verifyThat("#error", NodeMatchers.isInvisible());
        assertFalse(error.isManaged());

        @SuppressWarnings("unchecked")
        ChoiceBox<String> types = lookup("#types").queryAs(ChoiceBox.class);
        assertEquals(3, types.getItems().size());
        assertTrue(types.getItems().containsAll(List.of("Art", "Electronic", "Vehicle")));
    }

    // =========================================================================
    // 2. testAddItem_ValidInput – submit đúng → gửi SELL message với đủ dữ liệu
    // =========================================================================
    @Test
    public void testAddItem_ValidInput() {
        UserSession.getInstance().setUser(testUser);

        interact(() -> lookup("#submitButton").queryAs(Button.class).fire());

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService, times(1)).sendMessage(captor.capture());

        NetworkMessage sentMsg = captor.getValue();
        assertEquals("SELL", sentMsg.getAction());

        AuctionDTO auction = (AuctionDTO) sentMsg.getData();
        assertEquals("Laptop", auction.getItem().getName());
        assertEquals("A good laptop", auction.getItem().getDescription());
        assertEquals(100.0, auction.getStartingPrice());
        assertEquals(200.0, auction.getBuyOutPrice());
        assertEquals(10.0, auction.getTickSize());
        assertEquals(ItemType.ELECTRONICS, auction.getItem().getType());
        assertEquals(AuctionStatus.ACTIVE, auction.getStatus());
        assertEquals(testUser.getId(), auction.getItem().getOwner().getId());
    }

    // =========================================================================
    // 3. testAddItem_AntiSnippingEnabled – checkbox được chọn → antiSnipping=true
    // =========================================================================
    @Test
    public void testAddItem_AntiSnippingEnabled() {
        UserSession.getInstance().setUser(testUser);

        interact(() -> lookup("#antiSnippingCheckbox").queryAs(CheckBox.class).setSelected(true));
        interact(() -> lookup("#submitButton").queryAs(Button.class).fire());

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService, times(1)).sendMessage(captor.capture());

        AuctionDTO auction = (AuctionDTO) captor.getValue().getData();
        assertTrue(auction.isAntiSniping());
    }

    // =========================================================================
    // 4. testAddItem_MissingField – bỏ trống tên → hiện lỗi, không gửi message
    // =========================================================================
    @Test
    public void testAddItem_MissingField() {
        interact(() -> lookup("#productName").queryAs(TextField.class).setText(""));

        interact(() -> lookup("#submitButton").queryAs(Button.class).fire());

        verify(mockClientService, never()).sendMessage(any());

        Label error = lookup("#error").queryAs(Label.class);
        assertTrue(error.isVisible());
        assertTrue(error.isManaged());
        assertEquals("You missed some information", error.getText());
        assertEquals(Color.RED, error.getTextFill());
    }

    // =========================================================================
    // 5. testAddItem_NoTypeSelected – chưa chọn loại → hiện lỗi
    // =========================================================================
    @Test
    @SuppressWarnings("unchecked")
    public void testAddItem_NoTypeSelected() {
        interact(() -> lookup("#types").queryAs(ChoiceBox.class).setValue(null));

        interact(() -> lookup("#submitButton").queryAs(Button.class).fire());

        verify(mockClientService, never()).sendMessage(any());

        Label error = lookup("#error").queryAs(Label.class);
        assertTrue(error.isVisible());
        assertEquals("You have not selected item's type", error.getText());
        assertEquals(Color.RED, error.getTextFill());
    }

    // =========================================================================
    // 6. testAddItem_InvalidPrice_NonNumeric – giá không phải số → hiện lỗi
    // =========================================================================
    @Test
    public void testAddItem_InvalidPrice_NonNumeric() {
        interact(() -> lookup("#startingPrice").queryAs(TextField.class).setText("abc"));

        interact(() -> lookup("#submitButton").queryAs(Button.class).fire());

        verify(mockClientService, never()).sendMessage(any());

        Label error = lookup("#error").queryAs(Label.class);
        assertTrue(error.isVisible());
        assertEquals("Please enter valid numbers", error.getText());
        assertEquals(Color.RED, error.getTextFill());
    }

    // =========================================================================
    // 7. testAddItem_BuyoutLessThanStart – buyout <= start → lỗi
    // =========================================================================
    @Test
    public void testAddItem_BuyoutLessThanStart() {
        interact(() -> lookup("#buyoutPrice").queryAs(TextField.class).setText("50")); // buyout(50) <= start(100)

        interact(() -> lookup("#submitButton").queryAs(Button.class).fire());

        verify(mockClientService, never()).sendMessage(any());

        Label error = lookup("#error").queryAs(Label.class);
        assertTrue(error.isVisible());
        assertEquals("Buy Out Price is less than Start Price", error.getText());
        assertEquals(Color.RED, error.getTextFill());
    }

    // =========================================================================
    // 8. testAddItem_InvalidTickSize – tick không chia hết khoảng giá → lỗi
    // =========================================================================
    @Test
    public void testAddItem_InvalidTickSize() {
        // (200 - 100) = 100; tick = 30 → 100 % 30 != 0
        interact(() -> lookup("#tickSize").queryAs(TextField.class).setText("30"));

        interact(() -> lookup("#submitButton").queryAs(Button.class).fire());

        verify(mockClientService, never()).sendMessage(any());

        Label error = lookup("#error").queryAs(Label.class);
        assertTrue(error.isVisible());
        assertEquals("Invalid tick size", error.getText());
        assertEquals(Color.RED, error.getTextFill());
    }

    // =========================================================================
    // 9. testAddItem_ZeroTickSize – tick = 0 → lỗi (tickSizeVal <= 0)
    // =========================================================================
    @Test
    public void testAddItem_ZeroTickSize() {
        interact(() -> lookup("#tickSize").queryAs(TextField.class).setText("0"));

        interact(() -> lookup("#submitButton").queryAs(Button.class).fire());

        verify(mockClientService, never()).sendMessage(any());

        Label error = lookup("#error").queryAs(Label.class);
        assertTrue(error.isVisible());
        assertEquals("Invalid tick size", error.getText());
    }

    // =========================================================================
    // 10. testAddItem_InvalidBidDuration_NonInteger – duration không phải int
    // =========================================================================
    @Test
    public void testAddItem_InvalidBidDuration_NonInteger() {
        interact(() -> lookup("#bidDurationField").queryAs(TextField.class).setText("xyz"));

        interact(() -> lookup("#submitButton").queryAs(Button.class).fire());

        verify(mockClientService, never()).sendMessage(any());

        Label error = lookup("#error").queryAs(Label.class);
        assertTrue(error.isVisible());
        assertEquals("Please enter an integer", error.getText());
        assertEquals(Color.RED, error.getTextFill());
    }

    // =========================================================================
    // 11. testAddItem_NonPositiveBidDuration – duration <= 0 → lỗi
    // =========================================================================
    @Test
    public void testAddItem_NonPositiveBidDuration() {
        interact(() -> lookup("#bidDurationField").queryAs(TextField.class).setText("0"));

        interact(() -> lookup("#submitButton").queryAs(Button.class).fire());

        verify(mockClientService, never()).sendMessage(any());

        Label error = lookup("#error").queryAs(Label.class);
        assertTrue(error.isVisible());
        assertEquals("Bid duration must be positive", error.getText());
        assertEquals(Color.RED, error.getTextFill());
    }

    // =========================================================================
    // 12. testAddItem_VehicleType – chọn Vehicle → ItemType.VEHICLES
    // =========================================================================
    @Test
    @SuppressWarnings("unchecked")
    public void testAddItem_VehicleType() {
        UserSession.getInstance().setUser(testUser);

        interact(() -> lookup("#types").queryAs(ChoiceBox.class).setValue("Vehicle"));
        interact(() -> lookup("#submitButton").queryAs(Button.class).fire());

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService, times(1)).sendMessage(captor.capture());

        AuctionDTO auction = (AuctionDTO) captor.getValue().getData();
        assertEquals(ItemType.VEHICLES, auction.getItem().getType());
    }

    // =========================================================================
    // 13. testAddItem_ArtType – chọn Art → ItemType.ARTS
    // =========================================================================
    @Test
    @SuppressWarnings("unchecked")
    public void testAddItem_ArtType() {
        UserSession.getInstance().setUser(testUser);

        interact(() -> lookup("#types").queryAs(ChoiceBox.class).setValue("Art"));
        interact(() -> lookup("#submitButton").queryAs(Button.class).fire());

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService, times(1)).sendMessage(captor.capture());

        AuctionDTO auction = (AuctionDTO) captor.getValue().getData();
        assertEquals(ItemType.ARTS, auction.getItem().getType());
    }

    // =========================================================================
    // 14. testErrorClearedOnValidSubmit – submit lỗi trước, sau đó submit đúng → error ẩn
    // =========================================================================
    @Test
    public void testErrorClearedOnValidSubmit() {
        UserSession.getInstance().setUser(testUser);

        // Lần 1: để trống tên → error hiện
        interact(() -> lookup("#productName").queryAs(TextField.class).setText(""));
        interact(() -> lookup("#submitButton").queryAs(Button.class).fire());

        Label error = lookup("#error").queryAs(Label.class);
        assertTrue(error.isVisible());

        // Lần 2: điền lại đầy đủ → error ẩn, message được gửi
        interact(() -> lookup("#productName").queryAs(TextField.class).setText("Laptop"));
        interact(() -> lookup("#submitButton").queryAs(Button.class).fire());

        verify(mockClientService, times(1)).sendMessage(any());
    }

    // =========================================================================
    // 15. testAuctionStartPrice_EqualsCurrentPrice – currentPrice = startPrice khi tạo
    // =========================================================================
    @Test
    public void testAuctionStartPrice_EqualsCurrentPrice() {
        UserSession.getInstance().setUser(testUser);

        interact(() -> lookup("#submitButton").queryAs(Button.class).fire());

        ArgumentCaptor<NetworkMessage> captor = ArgumentCaptor.forClass(NetworkMessage.class);
        verify(mockClientService, times(1)).sendMessage(captor.capture());

        AuctionDTO auction = (AuctionDTO) captor.getValue().getData();
        assertEquals(100.0, auction.getStartingPrice());
        assertEquals(100.0, auction.getCurrentHighestBid());
    }
}

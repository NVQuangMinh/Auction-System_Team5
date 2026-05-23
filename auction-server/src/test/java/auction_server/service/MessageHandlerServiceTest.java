package auction_server.service;

import auction_server.dao.DAOProvider;
import auction_server.dao.UserDAO;
import auction_server.entities.User;
import auction_server.exception.UserBannedException;
import auction_server.exception.UserNotFoundException;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.SignUpDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho MessageHandlerService.
 *
 * CHIẾN LƯỢC TEST:
 * ================
 * MessageHandlerService có nhiều method, nhưng một số method gọi
 * AuctionManager.getInstance() — đây là Singleton khó mock.
 *
 * Ta tập trung test các method KHÔNG phụ thuộc AuctionManager:
 *   ✅ handleLogin       — chỉ dùng UserService + messageSender
 *   ✅ handleCreateAccount — chỉ dùng UserService + messageSender
 *   ✅ handleLogout      — chỉ clear state nội bộ
 *   ✅ setLoggedInUser / getLoggedInUser — getter/setter đơn giản
 *
 * CÁC METHOD BỎ QUA (cần AuctionManager):
 *   ❌ handlePlaceBid, handleBuyOut, handleSell, handleGetProducts, ...
 */
@DisplayName("MessageHandlerService Unit Tests")
class MessageHandlerServiceTest {

    // ── Dependencies (mock) ──────────────────────────────────────────────────
    private DAOProvider daoProvider;
    private UserDAO userDAO;
    private MessageHandlerService.MessageSender messageSender;

    // ── Object cần test ──────────────────────────────────────────────────────
    private MessageHandlerService service;

    // ── Capture message gửi đi ──────────────────────────────────────────────
    // Thay vì verify phức tạp, ta dùng biến này để lưu message cuối cùng được gửi
    private NetworkMessage lastSentMessage;

    @BeforeEach
    void setUp() {
        daoProvider    = mock(DAOProvider.class);
        userDAO        = mock(UserDAO.class);
        messageSender  = mock(MessageHandlerService.MessageSender.class);

        when(daoProvider.userDAO()).thenReturn(userDAO);

        // Capture message gửi đi để assert trong test
        // doAnswer: khi messageSender.sendMessage() được gọi, lưu argument vào lastSentMessage
        doAnswer(invocation -> {
            lastSentMessage = invocation.getArgument(0);
            return null;
        }).when(messageSender).sendMessage(any());

        service = new MessageHandlerService(messageSender, daoProvider);
    }

    // ════════════════════════════════════════════════════════════════════════
    // handleLogin
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("handleLogin - Đăng nhập thành công, gửi LOGIN với UserDTO")
    void testHandleLogin_Success() {
        // ARRANGE
        User mockUser = new User("u1", "alice", "pass123", "USER", "AVAILABLE");
        when(userDAO.getUserByUsername("alice")).thenReturn(mockUser);

        NetworkMessage msg = new NetworkMessage("LOGIN", new SignUpDTO(null, "alice", "pass123"));

        // ACT
        service.handleLogin(msg);

        // ASSERT
        // 1. User phải được set vào session
        assertEquals(mockUser, service.getLoggedInUser(), "User phải được lưu vào session sau login");

        // 2. Message gửi về client phải là "LOGIN" với data không null
        assertEquals("LOGIN", lastSentMessage.getAction());
        assertNotNull(lastSentMessage.getData(), "Data phải là UserDTO, không phải null");
    }

    @Test
    @DisplayName("handleLogin - Sai mật khẩu, gửi LOGIN với data null")
    void testHandleLogin_WrongPassword() {
        // ARRANGE
        User mockUser = new User("u1", "alice", "correctPass", "USER", "AVAILABLE");
        when(userDAO.getUserByUsername("alice")).thenReturn(mockUser);

        NetworkMessage msg = new NetworkMessage("LOGIN", new SignUpDTO(null, "alice", "wrongPass"));

        // ACT
        service.handleLogin(msg);

        // ASSERT
        assertNull(service.getLoggedInUser(), "User không được set vào session khi login thất bại");
        assertEquals("LOGIN", lastSentMessage.getAction());
        assertNull(lastSentMessage.getData(), "Data phải null khi login thất bại");
    }

    @Test
    @DisplayName("handleLogin - User bị ban, gửi LOGIN với data null")
    void testHandleLogin_UserBanned() {
        // ARRANGE
        User bannedUser = new User("u2", "bob", "pass", "USER", "BANNED");
        when(userDAO.getUserByUsername("bob")).thenReturn(bannedUser);

        NetworkMessage msg = new NetworkMessage("LOGIN", new SignUpDTO(null, "bob", "pass"));

        // ACT
        service.handleLogin(msg);

        // ASSERT
        assertNull(service.getLoggedInUser());
        assertNull(lastSentMessage.getData(), "Data phải null khi user bị ban");
    }

    @Test
    @DisplayName("handleLogin - User không tồn tại, gửi LOGIN với data null")
    void testHandleLogin_UserNotFound() {
        // ARRANGE
        when(userDAO.getUserByUsername("ghost")).thenReturn(null);

        NetworkMessage msg = new NetworkMessage("LOGIN", new SignUpDTO(null, "ghost", "pass"));

        // ACT
        service.handleLogin(msg);

        // ASSERT
        assertNull(service.getLoggedInUser());
        assertNull(lastSentMessage.getData());
    }

    // ════════════════════════════════════════════════════════════════════════
    // handleCreateAccount
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("handleCreateAccount - Tạo tài khoản thành công, gửi CREATE_ACCOUNT true")
    void testHandleCreateAccount_Success() {
        // ARRANGE
        // getUserByUsername trả về null → username chưa tồn tại → có thể đăng ký
        when(userDAO.getUserByUsername("newuser")).thenReturn(null);
        doNothing().when(userDAO).insertUser(any());

        NetworkMessage msg = new NetworkMessage("CREATE_ACCOUNT", new SignUpDTO("id1", "newuser", "pass123"));

        // ACT
        service.handleCreateAccount(msg);

        // ASSERT
        assertEquals("CREATE_ACCOUNT", lastSentMessage.getAction());
        assertEquals(true, lastSentMessage.getData(), "Data phải là true khi tạo tài khoản thành công");

        // Verify insertUser đã được gọi đúng 1 lần
        verify(userDAO, times(1)).insertUser(any());
    }

    @Test
    @DisplayName("handleCreateAccount - Username đã tồn tại, gửi CREATE_ACCOUNT false")
    void testHandleCreateAccount_DuplicateUsername() {
        // ARRANGE
        // getUserByUsername trả về user → username đã tồn tại
        User existing = new User("old", "newuser", "oldpass");
        when(userDAO.getUserByUsername("newuser")).thenReturn(existing);

        NetworkMessage msg = new NetworkMessage("CREATE_ACCOUNT", new SignUpDTO("id1", "newuser", "pass123"));

        // ACT
        service.handleCreateAccount(msg);

        // ASSERT
        assertEquals("CREATE_ACCOUNT", lastSentMessage.getAction());
        assertEquals(false, lastSentMessage.getData(), "Data phải là false khi username đã tồn tại");

        // insertUser KHÔNG được gọi
        verify(userDAO, never()).insertUser(any());
    }

    // ════════════════════════════════════════════════════════════════════════
    // handleLogout
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("handleLogout - Xóa loggedInUser về null")
    void testHandleLogout_ClearsSession() {
        // ARRANGE — set user trước
        service.setLoggedInUser(new User("u1", "alice", "pass"));
        assertNotNull(service.getLoggedInUser()); // đảm bảo user đang có

        // ACT
        service.handleLogout(new NetworkMessage("LOGOUT", null));

        // ASSERT
        assertNull(service.getLoggedInUser(), "loggedInUser phải là null sau khi logout");
    }

    // ════════════════════════════════════════════════════════════════════════
    // setLoggedInUser / getLoggedInUser
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setLoggedInUser / getLoggedInUser - Getter trả về đúng user đã set")
    void testSetAndGetLoggedInUser() {
        User user = new User("u1", "alice", "pass");
        service.setLoggedInUser(user);
        assertEquals(user, service.getLoggedInUser());
    }

    @Test
    @DisplayName("getLoggedInUser - Trả về null khi chưa login")
    void testGetLoggedInUser_InitiallyNull() {
        assertNull(service.getLoggedInUser());
    }
}

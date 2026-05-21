package auction_server.Network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import auction_server.core.AuctionManager;
import auction_server.dao.DAOProvider;
import auction_server.entities.User;
import auction_server.service.MessageHandlerService;
import auction_shared.Network.NetworkMessage;
import auction_shared.Network.Notification;

/**
 * Handler xử lý kết nối và giao tiếp với client.
 * 
 * Class này quản lý socket connection, nhận message từ client,
 * và delegate việc xử lý cho MessageHandlerService.
 * 
 * @author Team 5
 * @version 1.0
 * @see MessageHandlerService
 */
public class ClientHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);
    private List<Notification> activities = new ArrayList<>();
    private MessageHandlerService messageHandler;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;


    /**
     * Khởi tạo ClientHandler với socket connection.
     * 
     * @param socket Socket kết nối với client
     */
    public ClientHandler(Socket socket, DAOProvider daoProvider) {
        this.socket = socket;
        this.messageHandler = new MessageHandlerService(
                activities,
                this::sendMessage,
                daoProvider
        );
    }

    public User getLoggedInUser() {
        return messageHandler.getLoggedInUser();
    }

    /**
     * Chạy vòng lặp nhận và xử lý message từ client.
     */
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                NetworkMessage msg = (NetworkMessage) in.readObject();
                try {
                    handleRequest(msg);
                } catch (Exception e) {
                    log.error("Error handling request: {}", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            AuctionManager.getInstance().removeClient(this);
            log.info("Client has disconnected");
        }
    }

    public synchronized void sendMessage(NetworkMessage msg) {
        try {
            out.writeObject(msg);
            out.flush();
            out.reset();
        } catch (IOException e) {
            log.info("fail to send message", e);
        }
    }

    /**
     * Xử lý request từ client bằng cách phân loại action và delegate cho MessageHandlerService.
     * 
     * @param msg NetworkMessage từ client
     */
    private void handleRequest(NetworkMessage msg) {
        String action = msg.getAction();
        log.info("Handling request: {}, current activities size: {}", action, activities.size());
        
        switch (action) {
            case "PLACE_BID":
                messageHandler.handlePlaceBid(msg);
                break;
            case "SELL":
                messageHandler.handleSell(msg);
                break;
            case "LOGIN":
                messageHandler.handleLogin(msg);
                break;
            case "GET_PRODUCTS":
                messageHandler.handleGetProducts(msg);
                break;
            case "BUY_OUT":
                messageHandler.handleBuyOut(msg);
                break;
            case "GET_MY_LIST":
                messageHandler.handleGetMyList(msg);
                break;
            case "CREATE_ACCOUNT":
                messageHandler.handleCreateAccount(msg);
                break;
            case "GET_ACTIVITIES":
                messageHandler.handleGetActivities(msg);
                break;
            case "BAN_USER":
                messageHandler.handleBanUser(msg);
                break;
            case "REMOVE_ITEM":
                messageHandler.handleRemoveItem(msg);
                break;
            case "GET_USERS":
                messageHandler.handleGetUsers(msg);
                break;
            case "GET_BID_HISTORY":
                messageHandler.handleGetBidHistory(msg);
                break;
            case "LOGOUT":
                messageHandler.handleLogout(msg);
                break;
            default:
                log.warn("Unknown action: {}", action);
                break;
        }
    }

}

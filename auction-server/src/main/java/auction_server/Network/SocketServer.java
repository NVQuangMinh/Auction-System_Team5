package auction_server.Network;

import auction_server.dao.*;
import auction_server.dao.interfaces.*;
import auction_server.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer {
    private static final Logger log = LoggerFactory.getLogger(SocketServer.class);
    private final int port;
    private final int maxClients;
    private final ExecutorService clientPool;

    private final UserService userService;
    private final AuctionService auctionService;
    private final ItemService itemService;

    public SocketServer() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            props.load(input);
        } catch (IOException e) {
            log.error("Lỗi đọc file cấu hình");
        }

        this.port = Integer.parseInt(props.getProperty("server.port", "8080"));
        this.maxClients = Integer.parseInt(props.getProperty("server.max_clients", "100"));
        this.clientPool = Executors.newFixedThreadPool(maxClients);

        // Khởi tạo DAO
        UserDAO userDAO = new UserDAOImpl();
        ItemDAO itemDAO = new ItemDAOImpl();
        AuctionDAO auctionDAO = new AuctionDAOImpl();
        BidTransactionDAO bidTransactionDAO = new BidTransactionDAOImpl();

        // Khởi tạo Service
        this.userService = new UserService(userDAO);
        this.itemService = new ItemService(itemDAO, new ItemFactory());
        this.auctionService = new AuctionService(auctionDAO, itemDAO, bidTransactionDAO);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Server đang chạy trên port {}", port);
            while (true) {
                Socket socket = serverSocket.accept();
                clientPool.submit(new ClientHandler(socket, userService, auctionService, itemService));
            }
        } catch (IOException e) {
            log.error("Lỗi ServerSocket: ", e);
        }
    }
}
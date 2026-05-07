package auction_server.Network;

import auction_server.dao.AuctionDAOImpl;
import auction_server.dao.BidTransactionDAOImpl;
import auction_server.dao.ItemDAOImpl;
import auction_server.dao.UserDAOImpl;
import auction_server.dao.interfaces.AuctionDAO;
import auction_server.dao.interfaces.BidTransactionDAO;
import auction_server.dao.interfaces.ItemDAO;
import auction_server.dao.interfaces.UserDAO;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import auction_server.service.AuctionService;
import auction_server.service.ItemFactory;
import auction_server.service.ItemService;
import auction_server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SocketServer {

    private static final Logger log = LoggerFactory.getLogger(SocketServer.class);
    private final int port;
    private final int maxConcurrentClients;
    private final int shutdownTimeoutSeconds;

    private ServerSocket serverSocket;
    private ExecutorService clientPool;
    private volatile boolean isRunning = true;

    private final UserService userService;
    private final AuctionService auctionService;
    private final ItemService itemService; // Mới
    private final DataSource dataSource;

    public SocketServer() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Configuration file 'config.properties' not found in classpath.");
            }
            props.load(input);
        } catch (IOException ex) {
            throw new RuntimeException("Error reading configuration file.", ex);
        }

        this.port = Integer.parseInt(props.getProperty("server.port", "9999"));
        this.maxConcurrentClients = Integer.parseInt(props.getProperty("server.max_clients", "100"));
        this.shutdownTimeoutSeconds = Integer.parseInt(props.getProperty("server.shutdown_timeout_seconds", "30"));

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getProperty("db.url"));
        config.setUsername(props.getProperty("db.user"));
        config.setPassword(props.getProperty("db.password"));
        this.dataSource = new HikariDataSource(config);
        log.info("Database connection pool initialized.");

        UserDAO userDAO = new UserDAOImpl(dataSource);
        ItemDAO itemDAO = new ItemDAOImpl(dataSource);
        AuctionDAO auctionDAO = new AuctionDAOImpl(dataSource);
        BidTransactionDAO bidTransactionDAO = new BidTransactionDAOImpl(dataSource);
        log.info("DAO layer initialized.");

        // --- Khởi tạo Factory và các Service ---
        ItemFactory itemFactory = new ItemFactory();
        this.userService = new UserService(userDAO);
        this.itemService = new ItemService(itemDAO, itemFactory); // Mới
        this.auctionService = new AuctionService(auctionDAO, itemDAO, bidTransactionDAO);
        log.info("Service layer initialized.");
    }

    public void start() {
        this.clientPool = Executors.newFixedThreadPool(maxConcurrentClients);
        try {
            serverSocket = new ServerSocket(port);
            log.info("Server started on port: {}. Waiting for clients...", port);

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    log.info("Accepted connection from: {}", clientSocket.getRemoteSocketAddress());
                    // "Tiêm" tất cả các service cần thiết vào ClientHandler
                    ClientHandler clientHandler = new ClientHandler(clientSocket, userService, auctionService, itemService);
                    clientPool.submit(clientHandler);
                } catch (IOException e) {
                    if (isRunning) {
                        log.error("Error accepting client connection", e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Could not start server on port: " + port, e);
        } finally {
            log.info("Server accept loop has stopped.");
        }
    }

    public void stop() {
        log.info("Initiating graceful shutdown...");
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.error("Error closing server socket", e);
        }

        clientPool.shutdown();
        try {
            if (!clientPool.awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS)) {
                log.warn("Forcing shutdown as tasks did not finish in time.");
                clientPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            clientPool.shutdownNow();
        }

        log.info("Closing database connection pool.");
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }
        log.info("Shutdown complete.");
    }
}

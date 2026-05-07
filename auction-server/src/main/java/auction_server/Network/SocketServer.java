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
    private final DataSource dataSource;

    public SocketServer() {
        // --- Bước 1: Đọc cấu hình từ file .properties ---
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                log.error("Sorry, unable to find config.properties");
                throw new RuntimeException("Configuration file 'config.properties' not found in classpath.");
            }
            props.load(input);
        } catch (IOException ex) {
            log.error("Error reading config.properties", ex);
            throw new RuntimeException("Error reading configuration file.", ex);
        }

        this.port = Integer.parseInt(props.getProperty("server.port", "9999"));
        this.maxConcurrentClients = Integer.parseInt(props.getProperty("server.max_clients", "100"));
        this.shutdownTimeoutSeconds = Integer.parseInt(props.getProperty("server.shutdown_timeout_seconds", "30"));

        // --- Bước 2: Khởi tạo Connection Pool (DataSource) từ cấu hình ---
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getProperty("db.url"));
        config.setUsername(props.getProperty("db.user"));
        config.setPassword(props.getProperty("db.password"));
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        this.dataSource = new HikariDataSource(config);
        log.info("Database connection pool initialized.");

        // --- Bước 3: Khởi tạo tầng DAO và 'tiêm' DataSource vào ---
        UserDAO userDAO = new UserDAOImpl(dataSource);
        AuctionDAO auctionDAO = new AuctionDAOImpl(dataSource);
        ItemDAO itemDAO = new ItemDAOImpl(dataSource);
        BidTransactionDAO bidTransactionDAO = new BidTransactionDAOImpl(dataSource);
        log.info("DAO layer initialized.");

        // --- Bước 4: Khởi tạo tầng Service và 'tiêm' DAO vào ---
        this.userService = new UserService(userDAO);
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

                    ClientHandler clientHandler = new ClientHandler(clientSocket, userService, auctionService);
                    clientPool.submit(clientHandler);

                } catch (IOException e) {
                    if (isRunning) {
                        log.error("Error accepting client connection", e);
                    }
                    // Nếu isRunning là false, vòng lặp sẽ tự thoát
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
        
        // 1. Ngừng nhận kết nối mới
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // Gây ra exception ở accept() để thoát vòng lặp
            }
        } catch (IOException e) {
            log.error("Error closing server socket", e);
        }

        // 2. Yêu cầu thread pool ngừng nhận task mới và chờ các task cũ hoàn thành
        clientPool.shutdown(); // Ngừng nhận task mới
        try {
            // Chờ tối đa N giây cho các task đang chạy hoàn thành
            if (!clientPool.awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS)) {
                log.warn("Some tasks did not finish within the grace period. Forcing shutdown...");
                clientPool.shutdownNow(); // Buộc dừng nếu quá thời gian
            }
        } catch (InterruptedException e) {
            clientPool.shutdownNow();
        }

        // 3. Sau khi tất cả đã dừng, đóng pool kết nối CSDL
        log.info("All client tasks finished. Closing database connection pool.");
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource)dataSource).close();
        }
        
        log.info("Shutdown complete.");
    }
}

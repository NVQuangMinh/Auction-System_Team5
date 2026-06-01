package auctionserver.Network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import auctionserver.core.AuctionManager;
import auctionserver.dao.DAOProvider;
import auctionserver.dao.DefaultDAOProvider;

public class SocketServer {

    private static final Logger log = LoggerFactory.getLogger(SocketServer.class);
    private final DAOProvider daoProvider = new DefaultDAOProvider();

    public void start(int port) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
             ServerSocket serverSocket = new ServerSocket(port)) {

            log.info("Server đã khởi động tại địa chỉ: {}", port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                log.info("Một người dùng mới đã kết nối!");
                // add client vào list các client đang online
                ClientHandler clientHandler = new ClientHandler(clientSocket, daoProvider);
                AuctionManager.getInstance().addClient(clientHandler);
                executor.submit(clientHandler);
            }
        }
        catch (IOException e) {
            log.info("Server đã xảy ra lỗi", e);
        }
    }
}

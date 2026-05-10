package auction_server.Network;

import auction_server.core.AuctionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer {

    private static final Logger log = LoggerFactory.getLogger(SocketServer.class);


    public void start(int port) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
             ServerSocket serverSocket = new ServerSocket(port)) {

            log.info("Server is available at port: {}", port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                log.info("A new client connected!");
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                // add client vào list các client đang online
                AuctionManager.getInstance().addClient(clientHandler);
                executor.submit(clientHandler);
            }
        }
        catch (IOException e) {
            log.info("Server encountered an error", e);
        }
    }
}

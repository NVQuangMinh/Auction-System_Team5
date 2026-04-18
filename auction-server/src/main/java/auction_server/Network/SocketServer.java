package auction_server.Network;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer {
    public void start(int port) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
             ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server is available at port: " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("A new client connected!");
                executor.submit(new ClientHandler(clientSocket));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

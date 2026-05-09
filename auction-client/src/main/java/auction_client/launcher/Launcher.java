package auction_client.launcher;

import auction_client.Network.ClientService;
import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        try {
            ClientService clientService = ClientService.getInstance();
            String host = "localhost";
            int port = 8080;
            clientService.connect(host, port);
            System.out.println("Connected to server successfully!");
        } catch (Exception e) {
            System.err.println("Could not connect to server: " + e.getMessage());
            System.err.println("The application will continue without server connection.");
        }

        Application.launch(ClientLauncher.class, args);
    }
}

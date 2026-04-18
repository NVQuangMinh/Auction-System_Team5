package auction_client.launcher;

import auction_client.Network.ClientService;
import javafx.application.Application;

import java.io.IOException;

public class Launcher {
    public static void main(String[] args) {
        try{
            ClientService clientService = ClientService.getInstance();
            clientService.connect("localhost",8080);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        Application.launch(ClientLauncher.class, args);


    }
}

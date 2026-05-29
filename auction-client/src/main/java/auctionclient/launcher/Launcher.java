package auctionclient.launcher;

import auctionclient.Network.ClientService;
import auctionclient.controllers.notification.UserPushUpNotificationController;
import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        try {
            ClientService clientService = ClientService.getInstance();
            String host = "localhost";
            int port = 8080;
            clientService.connect(host, port);
            System.out.println("Connected to server successfully!");

            // Đăng ký bộ lắng nghe thông báo đẩy toàn cục ngay sau khi kết nối thành công
            clientService.addListener(new UserPushUpNotificationController());
        } catch (Exception e) {
            System.err.println("Could not connect to server: " + e.getMessage());
            System.err.println("The application will continue without server connection.");
        }

        Application.launch(ClientLauncher.class, args);
    }
}

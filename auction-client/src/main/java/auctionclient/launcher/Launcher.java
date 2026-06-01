package auctionclient.launcher;

import auctionclient.Network.ClientService;
import auctionclient.controllers.notification.UserPushUpNotificationController;
import javafx.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher {
    private static final Logger log = LoggerFactory.getLogger(Launcher.class);
    public static void main(String[] args) {
        try {
            ClientService clientService = ClientService.getInstance();
            String host = "localhost";
            int port = 8080;
            clientService.connect(host, port);
            log.info("Kết nối tới máy chủ thành công!");

            // Đăng ký bộ lắng nghe thông báo đẩy toàn cục ngay sau khi kết nối thành công
            clientService.addListener(new UserPushUpNotificationController());
        } catch (Exception e) {
            log.error("Không thể kết nối tới máy chủ: {}", e.getMessage());
            log.info("Phần mềm sẽ hoạt động mà không kết nối tới máy chủ.");
        }

        Application.launch(ClientLauncher.class, args);
    }
}

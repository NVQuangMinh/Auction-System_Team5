package auction_server.dao;

import auction_server.Network.ClientHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnection.class);

    // Cấu hình PostgreSQL
    private static final String URL = "jdbc:postgresql://localhost:5432/auction_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "811168";

    private static Connection connection = null;

    private DatabaseConnection() {
      //Singleton
    }

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                // Đăng ký driver (không bắt buộc với các bản JDBC mới nhưng nên có để ổn định)
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                log.info("Kết nối tới PostgreSQL thành công!");
            }
        } catch (ClassNotFoundException | SQLException e) {
            log.info("Lỗi kết nối Database: {}" ,e.getMessage());

        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                log.info("Đã đóng kết nối Database.");
            } catch (SQLException e) {
                log.info("Lỗi khi đóng kết nối: {}", e.getMessage());
            }
        }
    }
}

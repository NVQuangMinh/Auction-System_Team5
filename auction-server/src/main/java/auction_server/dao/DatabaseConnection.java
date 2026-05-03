package auction_server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Thay đổi thông tin theo cấu hình PostgreSQL của bạn
    private static final String URL = "jdbc:postgresql://localhost:5432/auction_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "your_password"; // Nhập mật khẩu của bạn ở đây

    private static Connection connection = null;

    private DatabaseConnection() {
        // Private constructor to prevent instantiation
    }

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                // Đăng ký driver (không bắt buộc với các bản JDBC mới nhưng nên có để ổn định)
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Kết nối tới PostgreSQL thành công!");
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Lỗi kết nối Database: " + e.getMessage());
            e.printStackTrace();
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Đã đóng kết nối Database.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

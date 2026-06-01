package auctionserver.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnection.class);

    // Dùng thư viện HikariCP để quản lý connection pool, tốc độ nhanh hơn so với 1 connection
    private static HikariDataSource dataSource = null;
    // Singleton
    private DatabaseConnection() { }

    private static synchronized void init() {
        if (dataSource != null) {
            return;
        }

        // đối tượng config ở đây là bản thiết kế lưu trữ thông tin kết nối đi đâu, dùng tài khoản nào,
        // hồ chứa được phép chứa tối đa bao nhiêu kết nối,...
        HikariConfig config = new HikariConfig();

        // dùng các biến môi trường để lấy thông tin DB
        config.setJdbcUrl(System.getenv("DB_URL"));
        config.setUsername(System.getenv("DB_USER"));
        config.setPassword(System.getenv("DB_PASSWORD"));
        config.setMaximumPoolSize(10);

        // nạp vào HikariDataSource để khởi chạy.
        dataSource = new HikariDataSource(config);
        log.info("Khởi tạo thành công danh sách kết nối cơ sở dữ liệu (Connection Pool).");
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            init();
        }
        return dataSource.getConnection();
    }

    public static void closePool() {
        if (dataSource != null) {
            dataSource.close();
            log.info("Đã đóng vùng kết nối cơ sở dữ liệu (Connection Pool).");
        }
    }
}

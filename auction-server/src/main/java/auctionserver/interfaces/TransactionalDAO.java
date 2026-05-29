package auctionserver.interfaces;

import java.sql.Connection;
import java.sql.SQLException;

public interface TransactionalDAO<T> {
    int insert(T t, Connection conn) throws SQLException;
}

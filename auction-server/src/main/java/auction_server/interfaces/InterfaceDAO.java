package auction_server.interfaces;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * @deprecated Thay thế bởi {@link ReadableDAO}, {@link WritableDAO},
 *             và {@link TransactionalDAO} và sẽ được xoá
 */
@Deprecated
public interface InterfaceDAO<T> {
    public int insert(T t);

    // Default method hỗ trợ Database Transaction
    default int insert(T t, Connection conn) throws SQLException {
        throw new UnsupportedOperationException("This DAO does not support transactional insert yet.");
    }

    public int delete(T t);

    public int update(T t);

    public ArrayList<T> selectAll();

    public T selectById(T t);

    public ArrayList<T> selectByCondition(String condition);
}

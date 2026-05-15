package auction_server.interfaces;

public interface WritableDAO<T> {
    int insert(T t);

    int delete(T t);

    int update(T t);
}

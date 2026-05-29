package auctionserver.interfaces;

import java.util.ArrayList;

public interface ReadableDAO<T> {
    ArrayList<T> selectAll();

    T selectById(T t);

    ArrayList<T> selectByCondition(String condition);
}

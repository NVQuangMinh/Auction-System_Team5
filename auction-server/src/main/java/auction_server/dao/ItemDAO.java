package auction_server.dao;

import auction_server.entities.Item;
import auction_server.interfaces.InterfaceDAO;

import java.util.ArrayList;

public class ItemDAO implements InterfaceDAO<Item> {

    @Override
    public int insert(Item item) {
        return 0;
    }

    @Override
    public int delete(Item item) {
        return 0;
    }

    @Override
    public int update(Item item) {
        return 0;
    }

    @Override
    public ArrayList<Item> selectAll() {
        return null;
    }

    @Override
    public Item selectById(Item item) {
        return null;
    }

    @Override
    public ArrayList<Item> selectByCondition(String condition) {
        return null;
    }
}

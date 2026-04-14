package entity;

public class Seller extends User {

    public Seller(String username, String passwordHash) {
        super(username, passwordHash);
    }

    @Override
    public String getRole() {
        return "SELLER";
    }

    public void createItem(Item item) {
        // TODO: Đẩy đối tượng entity.Item xuống DAO
    }
}
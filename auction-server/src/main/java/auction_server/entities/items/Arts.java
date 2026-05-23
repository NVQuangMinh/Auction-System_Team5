package auction_server.entities.items;

import auction_server.entities.Item;
import auction_server.entities.User;
import auction_shared.dto.ItemType;

public class Arts extends Item<String> {

    private String artistName;

    public Arts(String id, String itemName, String description, User owner, String artistName) {
        super(id, itemName, description, owner);
        this.artistName = artistName;
    }

    @Override
    public ItemType getType() {
        return ItemType.ARTS;
    }

    @Override
    public String getTypeSpecificAttribute() { return artistName; }

    @Override
    public void setTypeSpecificAttribute(String value) { this.artistName = value; }

    @Override
    public String getTypeAttributeLabel() { return "Artist"; }
}

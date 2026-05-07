package auction_server.entities.items;

import auction_server.entities.Item;
import auction_server.entities.User;

public class Art extends Item {
    private String artistName;

    public Art(Long id, String name, String description, User owner, String artistName) {
        super(id, name, description, owner);
        this.artistName = artistName;
    }

    public String getArtistName() {
        return artistName;
    }

    @Override
    public String getDetails() {
        return "Artist: " + artistName;
    }
}

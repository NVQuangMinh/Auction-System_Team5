module auction.shared {
    exports auction_shared.base;
    exports auction_shared.entities;
    exports auction_shared.interfaces;
    exports auction_shared.items;
    exports auction_shared.behaviors;

    opens auction_shared to auction.server, auction.client;
}
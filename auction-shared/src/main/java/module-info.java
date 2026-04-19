module auction.shared {
    exports auction_shared.base;
    exports auction_shared.entities;
    exports auction_shared.interfaces;
    exports auction_shared.items;
    exports auction_shared.behaviors;
    exports auction_shared.Network;

    opens auction_shared.entities to auction.server, auction.client;
    opens auction_shared.Network to auction.client, auction.server;
}
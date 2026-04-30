module auction.shared {
    exports auction_shared.base;
    exports auction_shared.dto;
    exports auction_shared.interfaces;
    exports auction_shared.behaviors;
    exports auction_shared.Network;

    opens auction_shared.dto to auction.server, auction.client;
    opens auction_shared.Network to auction.client, auction.server;
}
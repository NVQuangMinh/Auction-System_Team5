module auction.shared {
    exports auction_shared.dto;
    exports auction_shared.Network;

    opens auction_shared.dto to auction.server, auction.client;
    opens auction_shared.Network to auction.client, auction.server;
}
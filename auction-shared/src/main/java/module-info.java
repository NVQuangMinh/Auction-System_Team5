module auction.shared {
    exports auctionshared.dto;
    exports auctionshared.Network;

    opens auctionshared.dto to auction.server, auction.client;
    opens auctionshared.Network to auction.client, auction.server;
}
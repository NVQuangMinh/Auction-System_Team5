module auction.shared {
    exports auctionshared.dto;
    exports auctionshared.Network;

    opens auctionshared.dto;
    opens auctionshared.Network;
}
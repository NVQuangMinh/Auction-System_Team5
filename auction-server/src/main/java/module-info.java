module auction.server {

    requires auction.shared;
    requires java.sql;

    exports auction_server.core;
    opens auction_server.Network to auction.shared;
}
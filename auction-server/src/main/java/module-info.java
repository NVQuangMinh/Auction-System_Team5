module auction.server {

    requires auction.shared;
    requires java.sql;

    exports auction_server.core;
    exports auction_server.entities;
    exports auction_server.entities.items;
    exports auction_server.mapper;
    opens auction_server.Network to auction.shared;
}
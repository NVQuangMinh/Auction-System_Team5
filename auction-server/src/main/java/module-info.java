module auction.server {

    requires auction.shared;

    requires java.sql;

    exports auction_server.core;
}
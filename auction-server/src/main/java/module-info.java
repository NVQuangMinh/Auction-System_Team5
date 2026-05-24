module auction.server {

    requires transitive auction.shared;
    requires java.sql;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires com.zaxxer.hikari;

    exports auction_server.core;
    exports auction_server.entities;
    exports auction_server.entities.items;
    exports auction_server.behaviors;
    exports auction_server.interfaces;
    exports auction_server.mapper;
    exports auction_server.factory;
    opens auction_server.Network to auction.shared;
    exports auction_server.behaviors.profile;
    exports auction_server.service;

    // Cho phép Mockito truy cập các package này khi chạy test
    opens auction_server.dao;
    opens auction_server.entities;
    opens auction_server.service;
}
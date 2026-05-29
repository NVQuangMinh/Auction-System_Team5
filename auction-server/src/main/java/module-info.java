module auction.server {

    requires transitive auction.shared;
    requires java.sql;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires com.zaxxer.hikari;

    exports auctionserver.core;
    exports auctionserver.entities;
    exports auctionserver.entities.items;
    exports auctionserver.behaviors;
    exports auctionserver.interfaces;
    exports auctionserver.mapper;
    exports auctionserver.factory;
    opens auctionserver.Network to auction.shared;
    exports auctionserver.behaviors.profile;
    exports auctionserver.service;

    // Cho phép Mockito truy cập các package này khi chạy test
    opens auctionserver.dao;
    opens auctionserver.entities;
    opens auctionserver.service;
}
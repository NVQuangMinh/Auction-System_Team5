package auction_server.core;

import java.io.Serializable;

import auction_server.entities.Auction;
import auction_server.entities.User;
import auction_server.Network.ClientHandler;
import auction_server.dao.AuctionDAO;
import auction_server.mapper.Mappers;

import auction_shared.Network.NetworkMessage;
import auction_shared.dto.AuctionDTO;
import auction_shared.dto.AuctionStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionScheduler {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AuctionManager auctionManager;

    public AuctionScheduler(AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::checkExpiredAuctions, 0, 1, TimeUnit.SECONDS);
    }

    private void checkExpiredAuctions() {
        AuctionDAO auctionDAO = new AuctionDAO();
        for (Auction auction : auctionManager.getAllRooms()) {
            if (auction.isExpired()) {
                auction.endAuction();
                auctionDAO.update(auction);
                auctionManager.removeRoom(auction);
                AuctionDTO auctionDTO = Mappers.toDTO(auction);

                // gửi YOU WON cho winner
                String winnerId = auction.getWinnerId();
                if (winnerId != null) {
                    for (ClientHandler client : AuctionManager.getActiveClients()) {
                        User u = client.getLoggedInUser();
                        if (u != null && winnerId.equals(u.getId())) {
                            client.sendMessage(new NetworkMessage("YOU_WON or sth idk", auctionDTO));
                            break;
                        }
                    }
                }
                // Broadcast AUCTION_ENDED cho tất cả
                auctionManager.broadCast(new NetworkMessage("AUCTION_ENDED", auctionDTO));
                auctionManager.broadCast(new NetworkMessage("UPDATE_BID",
                        (Serializable) Mappers.toAuctionDTOList(auctionManager.getAllRooms())));
            }
        }
    }

    public void stop() {
        scheduler.shutdown();
    }
}
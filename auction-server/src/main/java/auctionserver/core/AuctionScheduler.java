package auctionserver.core;

import java.io.Serializable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import auctionserver.Network.ClientHandler;
import auctionserver.dao.AuctionDAO;
import auctionserver.dao.DAOProvider;
import auctionserver.entities.Auction;
import auctionserver.entities.User;
import auctionserver.mapper.Mappers;
import auctionserver.service.WinnerService;
import auctionshared.Network.NetworkMessage;
import auctionshared.dto.AuctionStatus;
import auctionshared.dto.AuctionDTO;

public class AuctionScheduler {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AuctionManager auctionManager;
    private final AuctionDAO auctionDAO;
    private final WinnerService winnerService;

    //giảm tải bằng cách tái sử dụng cache của AuctionDAO

    public AuctionScheduler(AuctionManager auctionManager, DAOProvider daoProvider) {
        this.auctionManager = auctionManager;
        this.auctionDAO = daoProvider.auctionDAO();
        this.winnerService = new WinnerService(daoProvider);
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::checkExpiredAuctions, 0, 1, TimeUnit.SECONDS);
    }

    private void checkExpiredAuctions() {
        for (Auction auction : auctionManager.getAllRooms()) {
            auction.getLock().lock();
            try {
                if (auction.isExpired() && auction.getStatus() == AuctionStatus.ACTIVE) {
                    auction.endAuction();
                    String winnerId = winnerService.determineWinner(auction.getBidHistory());
                    auction.setWinnerId(winnerId);
                    auctionDAO.update(auction);
                    AuctionDTO auctionDTO = Mappers.toDTO(auction);

                    if (winnerId != null) {
                        for (ClientHandler client : auctionManager.getActiveClients()) {
                            User u = client.getLoggedInUser();
                            if (u != null && winnerId.equals(u.getId())) {
                                client.sendMessage(new NetworkMessage("YOU_WON", auctionDTO));
                                break;
                            }
                        }
                    }

                    auctionManager.broadcast(new NetworkMessage("AUCTION_ENDED", auctionDTO));
                    // Chỉ broadcast ACTIVE từ RAM — client giữ ENDED/SOLD từ DB
                    var activeOnly = auctionManager.getAllRooms().stream()
                            .filter(a -> a.getStatus() == AuctionStatus.ACTIVE)
                            .collect(java.util.stream.Collectors.toList());
                    auctionManager.broadcast(new NetworkMessage("UPDATE_BID",
                            (Serializable) Mappers.toAuctionDTOList(activeOnly)));
                    // Xóa auction đã ENDED khỏi RAM — ENDED/SOLD serve từ DB
                    auctionManager.removeRoom(auction);
                }
            } finally {
                auction.getLock().unlock();
            }
        }
    }

    public void stop() {
        scheduler.shutdown();
    }
}
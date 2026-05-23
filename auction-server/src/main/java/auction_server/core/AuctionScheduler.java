package auction_server.core;

import java.io.Serializable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import auction_server.Network.ClientHandler;
import auction_server.dao.AuctionDAO;
import auction_server.dao.DAOProvider;
import auction_server.entities.Auction;
import auction_server.entities.User;
import auction_server.mapper.Mappers;
import auction_server.service.WinnerService;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.AuctionStatus;
import auction_shared.dto.AuctionDTO;

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
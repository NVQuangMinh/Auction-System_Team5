package auction_server.mapper;

import java.util.ArrayList;
import java.util.List;

import auction_server.entities.Auction;
import auction_server.entities.BidTransaction;
import auction_server.entities.Item;
import auction_server.entities.User;
import auction_shared.dto.AuctionDTO;
import auction_shared.dto.BidTransactionDTO;
import auction_shared.dto.ItemDTO;
import auction_shared.dto.UserDTO;

public class Mappers {

    public static UserDTO toDTO(User user) {
        if (user == null)
            return null;
        return new UserDTO(user.getId(), user.getUsername(),user.getRole());
    }

    @SuppressWarnings("rawtypes")
    public static ItemDTO toDTO(Item item) {
        if (item == null)
            return null;
        return new ItemDTO(
                item.getId(),
                item.getName(),
                item.getDescription(),
                toDTO(item.getOwner()),
                item.getType(),
                (String) item.getTypeSpecificAttribute());
    }

    public static AuctionDTO toDTO(Auction auction) {
        if (auction == null)
            return null;
        return new AuctionDTO(
                toDTO(auction.getItem()),
                auction.getStatus(),
                auction.getStartingPrice(),
                auction.getBuyOutPrice(),
                auction.getTickSize(),
                auction.getStartTime(),
                auction.getEndTime(),
                auction.isAntiSniping(),
                auction.getWinnerId(),
                auction.getCurrentHighestBid()
        );
    }

    public static BidTransactionDTO toDTO(BidTransaction transaction) {
        if (transaction == null)
            return null;
        return new BidTransactionDTO(
                toDTO(transaction.getAuction()),
                toDTO(transaction.getBidder()),
                transaction.getBidAmount(),
                transaction.getBidTime());
    }

    public static List<AuctionDTO> toAuctionDTOList(List<Auction> auctions) {
        List<AuctionDTO> dtos = new ArrayList<>();
        for (Auction auction : auctions) {
            dtos.add(toDTO(auction));
        }
        return dtos;
    }

    public static List<UserDTO> toUserDTOList(List<User> users) {
        List<UserDTO> dtos = new ArrayList<>();
        for (User user : users) {
            dtos.add(toDTO(user));
        }
        return dtos;
    }
    public static List<BidTransactionDTO> toBidTransactionDTOList(List<BidTransaction> bidHistory) {
        List<BidTransactionDTO> dtos = new ArrayList<>();
        for (BidTransaction transaction : bidHistory) {
            dtos.add(toDTO(transaction));
        }
        return dtos;
    }
}

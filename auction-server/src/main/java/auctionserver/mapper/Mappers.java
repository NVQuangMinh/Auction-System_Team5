package auctionserver.mapper;

import java.util.ArrayList;
import java.util.List;

import auctionserver.entities.Auction;
import auctionserver.entities.BidTransaction;
import auctionserver.entities.Item;
import auctionserver.entities.User;
import auctionshared.dto.AuctionDTO;
import auctionshared.dto.BidTransactionDTO;
import auctionshared.dto.ItemDTO;
import auctionshared.dto.UserDTO;

public class Mappers {

    public static UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        return new UserDTO(user.getId(), user.getUsername(), user.getRole());
    }

    @SuppressWarnings("rawtypes")
    public static ItemDTO toDTO(Item item) {
        if (item == null) {
            return null;
        }
        return new ItemDTO(
                item.getId(),
                item.getName(),
                item.getDescription(),
                toDTO(item.getOwner()),
                item.getType(),
                (String) item.getTypeSpecificAttribute());
    }

    public static AuctionDTO toDTO(Auction auction) {
        if (auction == null) {
            return null;
        }
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
        if (transaction == null) {
            return null;
        }
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

package auction_server.mapper;

import auction_server.entities.Auction;
import auction_server.entities.Item;
import auction_server.entities.User;
import auction_shared.dto.AuctionDTO;
import auction_shared.dto.ItemDTO;
import auction_shared.dto.UserDTO;

import java.util.List;
import java.util.stream.Collectors;

public class Mappers {

    public static UserDTO toUserDTO(User user) {
        if (user == null) return null;
        return new UserDTO(user.getId(), user.getUsername());
    }

    public static ItemDTO toItemDTO(Item item) {
        if (item == null) return null;
        return new ItemDTO(
                item.getId(),
                item.getName(),
                item.getDescription(),
                toUserDTO(item.getOwner()),
                item.getDetails() // Lấy thông tin chi tiết (brand, artist...)
        );
    }

    public static AuctionDTO toAuctionDTO(Auction auction) {
        if (auction == null) return null;
        return new AuctionDTO(
                auction.getId(),
                toItemDTO(auction.getItem()),
                auction.getStartingPrice(),
                auction.getBuyOutPrice(),      // Mới
                auction.getTickSize(),         // Mới
                auction.getCurrentHighestBid(),
                auction.getStatus(),
                auction.getStartTime(),
                auction.getEndTime()
        );
    }



    public static List<AuctionDTO> toAuctionDTOList(List<Auction> auctions) {
        return auctions.stream()
                .map(Mappers::toAuctionDTO)
                .collect(Collectors.toList());
    }
}

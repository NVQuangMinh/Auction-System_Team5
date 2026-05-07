package auction_client.interfaces;

import auction_shared.dto.AuctionDTO;

public interface HandleCardClicked {
    public void openAuctionDetail(AuctionDTO auctionDTO);
}

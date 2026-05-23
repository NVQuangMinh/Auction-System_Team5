package auction_shared.dto;

import java.io.Serializable;
import java.util.List;

public class ProductListResponse implements Serializable {
    private List<AuctionDTO> activeAuctions;
    private List<AuctionDTO> endedSaledAuctions;
    private int endedTotalCount;
    private int activeTotalCount;

    public ProductListResponse(List<AuctionDTO> activeAuctions,
                               List<AuctionDTO> endedSaledAuctions,
                               int endedTotalCount,
                               int activeTotalCount) {
        this.activeAuctions = activeAuctions;
        this.endedSaledAuctions = endedSaledAuctions;
        this.endedTotalCount = endedTotalCount;
        this.activeTotalCount = activeTotalCount;
    }

    public List<AuctionDTO> getActiveAuctions() {
        return activeAuctions;
    }

    public List<AuctionDTO> getEndedSaledAuctions() {
        return endedSaledAuctions;
    }

    public int getEndedTotalCount() {
        return endedTotalCount;
    }

    public int getActiveTotalCount() {
        return activeTotalCount;
    }
}
